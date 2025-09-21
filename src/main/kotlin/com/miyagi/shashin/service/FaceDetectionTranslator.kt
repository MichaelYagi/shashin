package com.miyagi.shashin.service

import ai.djl.modality.cv.Image
import ai.djl.modality.cv.output.*
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDArrays
import ai.djl.ndarray.NDList
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.DataType
import ai.djl.ndarray.types.Shape
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil


class FaceDetectionTranslator(
    private val confThresh: Double,
    private val nmsThresh: Double,
    private val variance: DoubleArray,
    private val topK: Int,
    private val scales: Array<IntArray>,
    private val steps: IntArray
) : Translator<Image?, DetectedObjects?> {
    /** {@inheritDoc}  */
    override fun processInput(ctx: TranslatorContext, input: Image?): NDList {
        ctx.setAttachment("width", input!!.width)
        ctx.setAttachment("height", input.height)

        var array = input.toNDArray(ctx.ndManager, Image.Flag.COLOR)
        array = array.transpose(2, 0, 1).flip(0) // HWC -> CHW RGB -> BGR
        // The network by default takes float32
        if (array.dataType != DataType.FLOAT32) {
            array = array.toType(DataType.FLOAT32, false)
        }
        val mean =
            ctx.ndManager.create(floatArrayOf(104f, 117f, 123f), Shape(3, 1, 1))
        array = array.sub(mean)
        return NDList(array)
    }

    /** {@inheritDoc}  */
    override fun processOutput(ctx: TranslatorContext, list: NDList): DetectedObjects {
        val width = ctx.getAttachment("width") as Int
        val height = ctx.getAttachment("height") as Int

        val manager = ctx.ndManager
        val scaleXY = variance[0]
        val scaleWH = variance[1]

        var prob = list[1][":, 1:"]
        prob =
            NDArrays.stack(
                NDList(
                    prob.argMax(1).toType(DataType.FLOAT32, false),
                    prob.max(intArrayOf(1))
                )
            )

        val boxRecover = boxRecover(manager, width, height, scales, steps)
        var boundingBoxes = list[0]
        val bbWH = boundingBoxes[":, 2:"].mul(scaleWH).exp().mul(boxRecover[":, 2:"])
        val bbXY =
            boundingBoxes[":, :2"]
                .mul(scaleXY)
                .mul(boxRecover[":, 2:"])
                .add(boxRecover[":, :2"])
                .sub(bbWH.mul(0.5f))

        boundingBoxes = NDArrays.concat(NDList(bbXY, bbWH), 1)

        var landms = list[2]
        landms = decodeLandm(landms, boxRecover, scaleXY)

        // filter the result below the threshold
        val cutOff = prob[1].gt(confThresh)
        boundingBoxes = boundingBoxes.transpose().booleanMask(cutOff, 1).transpose()
        landms = landms.transpose().booleanMask(cutOff, 1).transpose()
        prob = prob.booleanMask(cutOff, 1)

        // start categorical filtering
        val order = prob[1].argSort()[":$topK"].toLongArray()
        prob = prob.transpose()
        val retNames: MutableList<String> = ArrayList()
        val retProbs: MutableList<Double> = ArrayList()
        val retBB: MutableList<BoundingBox> = ArrayList()

        val recorder: MutableMap<Int, MutableList<BoundingBox>> = ConcurrentHashMap()

        for (i in order.indices.reversed()) {
            val currMaxLoc = order[i]
            val classProb = prob[currMaxLoc].toFloatArray()
            val classId = classProb[0].toInt()
            val probability = classProb[1].toDouble()

            val boxArr = boundingBoxes[currMaxLoc].toDoubleArray()
            val landmsArr = landms[currMaxLoc].toDoubleArray()
            val rect = Rectangle(boxArr[0], boxArr[1], boxArr[2], boxArr[3])
            val boxes = recorder.getOrDefault(classId, ArrayList())
            var belowIoU = true
            for (box in boxes) {
                if (box.getIoU(rect) > nmsThresh) {
                    belowIoU = false
                    break
                }
            }
            if (belowIoU) {
                val keyPoints: MutableList<Point> = ArrayList()
                for (j in 0..4) { // 5 face landmarks
                    val x = landmsArr[j * 2]
                    val y = landmsArr[j * 2 + 1]
                    keyPoints.add(Point(x * width, y * height))
                }
                val landmark =
                    Landmark(boxArr[0], boxArr[1], boxArr[2], boxArr[3], keyPoints)

                boxes.add(landmark)
                recorder[classId] = boxes
                val className = "Face" // classes.get(classId)
                retNames.add(className)
                retProbs.add(probability)
                retBB.add(landmark)
            }
        }

        return DetectedObjects(retNames, retProbs, retBB)
    }

    private fun boxRecover(
        manager: NDManager, width: Int, height: Int, scales: Array<IntArray>, steps: IntArray
    ): NDArray {
        val aspectRatio = Array(steps.size) { IntArray(2) }
        for (i in steps.indices) {
            val wRatio = ceil((width.toFloat() / steps[i]).toDouble()).toInt()
            val hRatio = ceil((height.toFloat() / steps[i]).toDouble()).toInt()
            aspectRatio[i] = intArrayOf(hRatio, wRatio)
        }

        val defaultBoxes: MutableList<DoubleArray> = ArrayList()

        for (idx in steps.indices) {
            val scale = scales[idx]
            for (h in 0 until aspectRatio[idx][0]) {
                for (w in 0 until aspectRatio[idx][1]) {
                    for (i in scale) {
                        val skx = i * 1.0 / width
                        val sky = i * 1.0 / height
                        val cx = (w + 0.5) * steps[idx] / width
                        val cy = (h + 0.5) * steps[idx] / height
                        defaultBoxes.add(doubleArrayOf(cx, cy, skx, sky))
                    }
                }
            }
        }

        val boxes = Array(defaultBoxes.size) {
            DoubleArray(
                defaultBoxes[0].size
            )
        }
        for (i in defaultBoxes.indices) {
            boxes[i] = defaultBoxes[i]
        }
        return manager.create(boxes).clip(0.0, 1.0)
    }

    // decode face landmarks, 5 points per face
    private fun decodeLandm(pre: NDArray, priors: NDArray, scaleXY: Double): NDArray {
        val point1 =
            pre[":, :2"].mul(scaleXY).mul(priors[":, 2:"]).add(priors[":, :2"])
        val point2 =
            pre[":, 2:4"].mul(scaleXY).mul(priors[":, 2:"]).add(priors[":, :2"])
        val point3 =
            pre[":, 4:6"].mul(scaleXY).mul(priors[":, 2:"]).add(priors[":, :2"])
        val point4 =
            pre[":, 6:8"].mul(scaleXY).mul(priors[":, 2:"]).add(priors[":, :2"])
        val point5 =
            pre[":, 8:10"].mul(scaleXY).mul(priors[":, 2:"]).add(priors[":, :2"])
        return NDArrays.concat(NDList(point1, point2, point3, point4, point5), 1)
    }
}