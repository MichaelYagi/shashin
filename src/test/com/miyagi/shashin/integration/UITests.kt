package com.miyagi.shashin.integration

import com.miyagi.shashin.e2e.BaseSeleniumTests
import com.miyagi.shashin.util.TextUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.openqa.selenium.JavascriptExecutor
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UITests: BaseSeleniumTests() {
    @LocalServerPort
    private val port = 0

    @Test
    @Throws(Exception::class)
    fun shouldSeeToastMessage() {
        this.driver!!.get("http://localhost:$port/testgrounds")
        val js: JavascriptExecutor = this.driver as JavascriptExecutor
        var hasToast = js.executeScript("shashin.showToastMessage('Title 1', 'Message 1.',{autohide:false,tag:\"test1\",placement:shashin.toast.placement.top.left});" +
                "return shashin.hasToast(shashin.toast.placement.top.left);")

        Assertions.assertTrue(hasToast as Boolean)

        hasToast = js.executeScript("return shashin.hasToast(shashin.toast.placement.top.center);")

        Assertions.assertFalse(hasToast as Boolean)

        hasToast = js.executeScript("shashin.closeToastMessages({placement:shashin.toast.placement.top.left});" +
                "return shashin.hasToast(shashin.toast.placement.top.left);")

        Assertions.assertFalse(hasToast as Boolean)
    }

    @Test
    @Throws(Exception::class)
    fun shouldCloseToastMessage() {
        this.driver!!.get("http://localhost:$port/testgrounds")
        val js: JavascriptExecutor = this.driver as JavascriptExecutor
        var hasToast = js.executeScript("shashin.showToastMessage('Title 1', 'Message 1.',{autohide:false,tag:\"test1\",placement:shashin.toast.placement.top.left});" +
                "return shashin.hasToast(shashin.toast.placement.top.left);")

        Assertions.assertTrue(hasToast as Boolean)

        hasToast = js.executeScript("shashin.closeToastMessages({placement:shashin.toast.placement.top.left,tag:\"test1\",hide:true});" +
                "return shashin.hasToast(shashin.toast.placement.top.left,{tag:\"test1\"});")

        Assertions.assertFalse(hasToast as Boolean)

        hasToast = js.executeScript("return shashin.hasToast(shashin.toast.placement.top.left,{findHidden:true});")

        Assertions.assertTrue(hasToast as Boolean)

        hasToast = js.executeScript("shashin.closeToastMessages({placement:shashin.toast.placement.top.left,tag:\"test1\"});" +
                "return shashin.hasToast(shashin.toast.placement.top.left,{findHidden:true});")

        Assertions.assertFalse(hasToast as Boolean)
    }

    @Test
    @Throws(Exception::class)
    fun equalDateTranslations() {
        this.driver!!.get("http://localhost:$port/testgrounds")
        val js: JavascriptExecutor = this.driver as JavascriptExecutor
        var jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"pt\")")
        var longDate = TextUtils.formatToLongDate("2021-10-17", "pt")
        Assertions.assertEquals(jsFormattedDate, longDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"fr\")")
        longDate = TextUtils.formatToLongDate("2021-10-17", "fr")
        Assertions.assertEquals(jsFormattedDate, longDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"ja\")")
        longDate = TextUtils.formatToLongDate("2021-10-17", "ja")
        Assertions.assertEquals(jsFormattedDate, longDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"es\")")
        longDate = TextUtils.formatToLongDate("2021-10-17", "es")
        Assertions.assertEquals(jsFormattedDate, longDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"de\")")
        longDate = TextUtils.formatToLongDate("2021-10-17", "de")
        Assertions.assertEquals(jsFormattedDate, longDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,12,1, \"ja\")")
        longDate = TextUtils.formatToLongDate("2021-12-01", "ja")
        Assertions.assertEquals(jsFormattedDate, longDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,12,1, \"pt\")")
        longDate = TextUtils.formatToLongDate("2021-12-01", "pt")
        Assertions.assertEquals(jsFormattedDate, longDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,12,1, \"pt\",false)")
        longDate = TextUtils.formatToLongDate("2021-12-01", "pt")
        Assertions.assertNotEquals(jsFormattedDate, longDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,12,1, \"pt\",false)")
        var shortDate = TextUtils.formatToAbbrDate("2021-12-01", "pt")
        Assertions.assertEquals(jsFormattedDate, shortDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"fr\",false)")
        shortDate = TextUtils.formatToAbbrDate("2021-10-17", "fr")
        Assertions.assertEquals(jsFormattedDate, shortDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"ja\",false)")
        shortDate = TextUtils.formatToAbbrDate("2021-10-17", "ja")
        Assertions.assertEquals(jsFormattedDate, shortDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"es\",false)")
        shortDate = TextUtils.formatToAbbrDate("2021-10-17", "es")
        Assertions.assertEquals(jsFormattedDate, shortDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,10,17, \"de\",false)")
        shortDate = TextUtils.formatToAbbrDate("2021-10-17", "de")
        Assertions.assertEquals(jsFormattedDate, shortDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,12,1, \"ja\",false)")
        shortDate = TextUtils.formatToAbbrDate("2021-12-01", "ja")
        Assertions.assertEquals(jsFormattedDate, shortDate)

        jsFormattedDate = js.executeScript("return Util.getDateString(2021,12,1, \"pt\",false)")
        shortDate = TextUtils.formatToAbbrDate("2021-12-01", "pt")
        Assertions.assertEquals(jsFormattedDate, shortDate)
    }

    @Test
    @Throws(Exception::class)
    fun shouldHavePagination() {
        this.driver!!.get("http://localhost:$port/testgrounds")

        val currentPage = 6
        val totalPages = 12

        val js: JavascriptExecutor = this.driver as JavascriptExecutor

        // Create nav element
        js.executeScript("$($('<nav></nav>').attr('id','pagination').append($('<ul></ul>').addClass('pagination'))).appendTo('main');")

        // Setup pagy options
        js.executeScript("const options = {\n" +
            "   currentPage: "+currentPage+",\n" +
            "   totalPages: "+totalPages+",\n" +
            "   truncate: true,\n" +
            "   innerWindow: 3,\n" +
            "   outerWindow: 1,\n" +
            "   first: null,\n" +
            "   last: null,\n" +
            "   href: function (index) {\n" +
            "       return '/test/' + (index+1);\n" +
            "   }\n" +
            "};" +
            "$('#pagination').pagy(options);"
        )

        // Count number of li elements
        var liCount = js.executeScript("return document.getElementById('pagination').getElementsByTagName('li').length;")
        Assertions.assertTrue(liCount!!.toString().toInt() == (totalPages+1))

        var notActiveClass = js.executeScript("return document.getElementById('pagination').firstChild.childNodes[3].className;")
        Assertions.assertTrue(notActiveClass == "page-item")

        var activeClass = js.executeScript("return document.getElementById('pagination').firstChild.childNodes[$currentPage].className;")
        Assertions.assertTrue(activeClass == "page-item active")

        js.executeScript("$($('<input type=\"text\" id=\"pagyPage\">')).appendTo('main');")
        js.executeScript("$($('<nav></nav>').attr('id','pagination2').append($('<ul></ul>').addClass('pagination'))).appendTo('main');")
        js.executeScript("const options = {\n" +
                "   currentPage: "+currentPage+",\n" +
                "   totalPages: "+totalPages+",\n" +
                "   page: function (page) {\n" +
                "       document.getElementById('pagyPage').value = page\n" +
                "   }\n" +
                "};" +
                "$('#pagination2').pagy(options);"
        )
        js.executeScript("document.getElementById('pagination2').firstChild.childNodes[4].firstChild.click()")
        activeClass = js.executeScript("return document.getElementById('pagination').firstChild.childNodes[$currentPage].className;")
        Assertions.assertTrue(activeClass == "page-item active")
        var pagyPage = js.executeScript("return document.getElementById('pagyPage').value")
        Assertions.assertTrue(pagyPage?.toString()?.toInt() == currentPage)
    }
}