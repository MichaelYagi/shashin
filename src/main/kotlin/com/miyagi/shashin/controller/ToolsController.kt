package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.javascript.jscomp.*
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader


@Controller
@Secured("ROLE_ADMIN")
class ToolsController {
    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @RequestMapping(value = ["tools/minifyassets"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getMinifyAssets(model: Model): String {
        val response = mutableMapOf<String, Any?>()

//        var input = "static/js/site/app.js"
//        var ouput = "static/js/site/app.min.js"
//        var resource = ClassPathResource("static/js/site/app.js")
//        var fis = FileInputStream(resource.file)
//        var fileContents = getFileContent(fis,"UTF-8")
//        var compiled = compile(fileContents)
//
//        println(compiled)

        response["msg"] = "success"
        response["message"] = "Success"

        return mapper.writeValueAsString(response)
    }

    private fun compile(code: String?): String? {
        val compiler = Compiler()
        val options = CompilerOptions()
        // Advanced mode is used here, but additional options could be set, too.
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(
            options
        )

        // To get the complete set of externs, the logic in
        // CompilerRunner.getDefaultExterns() should be used here.
        val extern = SourceFile.fromCode(
            "externs.js",
            "function alert(x) {}"
        )

        // The dummy input name "input.js" is used here so that any warnings or
        // errors will cite line numbers in terms of input.js.
        val input = SourceFile.fromCode("input.js", code)

        // compile() returns a Result, but it is not needed here.
        compiler.compile(extern, input, options)

        // The compiler is responsible for generating the compiled code; it is not
        // accessible via the Result.
        return compiler.toSource()
    }

    @Throws(IOException::class)
    private fun getFileContent(
        fis: FileInputStream,
        encoding: String
    ): String {
        BufferedReader(InputStreamReader(fis, encoding)).use { br ->
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append('\n')
            }
            return sb.toString()
        }
    }
}