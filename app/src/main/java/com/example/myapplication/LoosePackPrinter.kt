package com.example.myapplication

import android.content.Context
import android.icu.text.CaseMap.Title
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import org.xml.sax.InputSource
import java.io.BufferedReader
import java.io.OutputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


class LoosePackPrinter : AppCompatActivity() {
    private lateinit var in_barcode:EditText
    private lateinit var in_part:TextView
    private lateinit var in_batch:TextView
    private lateinit var in_quantity:TextView
    private lateinit var in_carton_number:TextView
    private lateinit var rb_printer1:RadioButton
    private lateinit var rb_printer2:RadioButton
    private lateinit var in_newQty:EditText
    private lateinit var badgeNum: String
    private lateinit var btn_print: Button
    private lateinit var pb:ProgressBar
    private lateinit var title:TextView
    private lateinit var c:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loose_pack_printer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        c = this@LoosePackPrinter
        badgeNum = intent.getStringExtra("Badge").toString()
        in_barcode = findViewById(R.id.PLPEDBarcode)
        in_part = findViewById(R.id.PLPTVPart)
        in_batch = findViewById(R.id.PLPTVBatch)
        in_quantity = findViewById(R.id.PLPTVQty)
        in_carton_number = findViewById(R.id.PLPTVCarton)
        rb_printer1 = findViewById(R.id.PLPrbPrinter1)
        rb_printer2 = findViewById(R.id.PLPrbPrinter2)
        in_newQty = findViewById(R.id.PLPEDnewQty)
        btn_print = findViewById(R.id.PLPbtnPrint)
        pb = findViewById(R.id.PLPPB)
        title = findViewById(R.id.LPTITLE)
        in_barcode.requestFocus()
        title.text = intent.getStringExtra("Desc")
        in_barcode.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
                || keyCode == KeyEvent.KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                CoroutineScope(Dispatchers.Main).launch {
                    var translatedBC = translateBarcode(in_barcode.text.toString())
                    var part_no = JSONObject(translatedBC).getString("material")
                    in_part.text = "Part : ${part_no}"
                    var batch_no = JSONObject(translatedBC).getString("batch")
                    in_batch.text = "Batch : ${batch_no}"
                    var carton_no = JSONObject(translatedBC).getString("reelnumber")
                    in_carton_number.text = "Carton Number : ${carton_no}"
                    var carton_quantity = JSONObject(translatedBC).getString("quantity")
                    in_quantity.text = "Quantity : ${carton_quantity}"
                    in_newQty.requestFocus()
                }


                return@OnKeyListener true
            }
            false
        })
        fun isNumber(input: String): Boolean {
            return input.toDoubleOrNull() != null
        }


        btn_print.setOnClickListener {
            var printerIP = String()
            printerIP = if(rb_printer1.isChecked){
                "172.16.208.92"
            } else{
                "172.16.208.150"
            }
            CoroutineScope(Dispatchers.Main).launch {
                if(isNumber(in_newQty.text.toString())){
                    var XMLresult = printToPrinter(in_barcode.text.toString(),printerIP,badgeNum)
                    val extractedStrings = translateXML(XMLresult)
                    showDialog(c,"Message","Part number : ${extractedStrings[0]} , \n New Quantity : ${extractedStrings[1]}")
                }
                else{
                    showDialog(c,"Message","Quantity tak betui")
                }

            }

        }


    }

    private fun translateXML(xmlResponse:String):List<String>{
        val result = mutableListOf<String>()
        try{
            // Parse the XML string into a Document
            val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlResponse))
            val document: Document = documentBuilder.parse(inputSource)
            // Use XPath to extract all <string> elements
            val xPath = XPathFactory.newInstance().newXPath()
            val expression = "//string" // XPath to find all <string> elements
            val nodeList = xPath.evaluate(expression, document, XPathConstants.NODESET) as NodeList

            // Iterate through the NodeList and collect the text content
            for (i in 0 until nodeList.length) {
                val nodeValue = nodeList.item(i).textContent
                result.add(nodeValue)
            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }
        return result
    }

    private suspend fun printToPrinter(barcodeString:String,printerIPAddress:String,badgeNumber:String):String{
        val soapRequest = """
        <?xml version="1.0" encoding="utf-8"?>
        <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <print_loose_pack_label xmlns="http://tempuri.org/">
              <barcode>${barcodeString}</barcode>
              <new_QTY>${in_newQty.text}</new_QTY>
              <printer_IP>${printerIPAddress}</printer_IP>
              <badgeNo>${badgeNumber}</badgeNo>
            </print_loose_pack_label>
          </soap:Body>
        </soap:Envelope>
    """.trimIndent()
        val url = URL("http://172.16.206.29/MOBILE_PRINTER/MOBILE_PRINTER.asmx")
        val connection = url.openConnection() as HttpURLConnection

        return withContext(Dispatchers.IO){
            try{
                // Show progress bar
                withContext(Dispatchers.Main) {
                    showProgressBar(true)
                }
                // Configure the connection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
                connection.setRequestProperty("SOAPAction", "http://tempuri.org/print_loose_pack_label")
                connection.doOutput = true

                // Send the SOAP request
                connection.outputStream.use { outputStream: OutputStream ->
                    outputStream.write(soapRequest.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }

                // Read the response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    response
                } else {
                    "Error code : ${responseCode}"
                }



            }
            catch (e:Exception){

                e.message.toString()

            }
            finally {
                withContext(Dispatchers.Main) {
                    showProgressBar(false)
                }
                connection.disconnect()
            }

        }
    }

    private suspend fun translateBarcode( barcodeString:String):String{
        return withContext(Dispatchers.IO){
            try{
                // Show progress bar
                withContext(Dispatchers.Main) {
                    showProgressBar(true)
                }

                val url = URL("http://172.16.206.19/FORD_SYNC/API/BARCODETRANSLATOR")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                val jsobBody = "\"${barcodeString}\""
                // Write the JSON body
                connection.outputStream.use { outputStream: OutputStream ->
                    outputStream.write(jsobBody.toByteArray(Charsets.UTF_8))
                }
                // Read the response
                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                        ?: "Error occurred with code: $responseCode"
                }

                // Hide progress bar
                withContext(Dispatchers.Main) {
                    showProgressBar(false)
                }
                response


            } catch (e: Exception) {
                e.message ?: "An unknown error occurred."
            }
        }
    }

    private fun clearForm(){
        in_barcode.text.clear()
        in_newQty.text.clear()
        in_part.text = "Part :"
        in_batch.text = "Batch :"
        in_quantity.text = "Quantity :"
        in_carton_number.text = "Carton Number : "
    }

    private fun showDialog(context: Context, title: String, message: String) {
        runOnUiThread(Runnable {
            clearForm()
            AlertDialog.Builder(context).apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                create()
                show()
            }
        })

    }


    private fun showProgressBar(isVisible: Boolean) {
        runOnUiThread(Runnable{
            if (isVisible) {
                pb.visibility = View.VISIBLE
            } else {
                pb.visibility = View.GONE
            }
        })

    }

}