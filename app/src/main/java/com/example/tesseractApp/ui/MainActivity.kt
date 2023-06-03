package com.example.tesseractApp.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.tesseractApp.databinding.ActivityMainBinding
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK){
            val data = it.data
            if (data != null && data.data != null){
                try{
                    var selectedImageBitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                        this.contentResolver, data.data!!
                    ))
                    selectedImageBitmap = selectedImageBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    mainBinding.ocrImage.setImageBitmap(selectedImageBitmap)
                    selectedImageName = selectedImageBitmap
                    mainBinding.ocrText.text = doOCR(selectedImageName)
                } catch (e: IOException) {
                    Log.e("MainActivity", "Failed to open a test image")
                }
            }
        }
    }
    private lateinit var mainBinding : ActivityMainBinding
    private lateinit var selectedImageName: Bitmap
    private lateinit var mTessOCR: TessBaseAPI
    private var srcFile = "eng.traineddata"
    private var lang = "eng"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        mainBinding.scanButton.setOnClickListener { imageChooser() }
        mainBinding.langOptionList.setOnCheckedChangeListener { _, checkedId -> languageChooser(checkedId) }
        initTesseract()
    }

    private fun imageChooser(){
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryActivityResultLauncher.launch(galleryIntent)
    }

    private fun initTesseract(){
        mTessOCR = TessBaseAPI()
        var fileExistFlag = false
        val dstPathDir = ""+applicationContext.filesDir + "/tesseract/tessdata/"
        lateinit var inFile:InputStream
        lateinit var  outFile: FileOutputStream
        val dstInitPathDir = ""+applicationContext.filesDir + "/tesseract/"
        val dstPathFile = dstPathDir + srcFile

        try{
            inFile = applicationContext.assets.open(srcFile)
            val file = File(dstPathDir)
            if (!file.exists()){
                if (!file.mkdirs()){
                    Toast.makeText(applicationContext, "$srcFile can't be created.", Toast.LENGTH_SHORT).show()
                }
                outFile = FileOutputStream(dstPathFile)
            }
            else {
                for (trainedData in applicationContext.filesDir.listFiles()[0].listFiles()[0].listFiles())
                    if (dstPathFile == "$trainedData")
                        fileExistFlag = true
                if (!fileExistFlag) outFile = FileOutputStream(dstPathFile)
            }
        } catch (ex:Exception) {Log.e("MainActivity", ex.message!!)}
        finally {
            if (fileExistFlag){
                try{
                    if (inFile != null) inFile.close()
                    mTessOCR.init(dstInitPathDir, lang)
                } catch (ex:Exception){Log.e("MainActivity", ex.message!!)}
            }
            else {
                if (inFile != null && outFile != null) {
                    try{
                        //copy file
                        val buf = ByteArray(1024)
                        var len: Int
                        while (inFile.read(buf).also { len = it } != -1) {
                            outFile.write(buf, 0, len)
                        }
                        inFile.close()
                        outFile.close()
                        mTessOCR.init(dstInitPathDir,lang)
                    } catch (ex:Exception){Log.e("MainActivity", ex.message!!)}
                }
                else Toast.makeText(applicationContext, "$srcFile can't be read.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doOCR(imageBitmap:Bitmap):String {
        mTessOCR.setImage(imageBitmap)
        return mTessOCR.utF8Text
    }

    private fun languageChooser(checkedId:Int) {
        val checkedLang = findViewById<RadioButton>(checkedId).text
        if (checkedLang == "Rus"){
            srcFile = "rus.traineddata"
            lang = "rus"
        }
        else if(checkedLang == "Eng"){
            srcFile = "eng.traineddata"
            lang = "eng"
        }
        mTessOCR.end()
        initTesseract()
    }
}