package app.harshit.offlinetranslate

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.Frame
import com.otaliastudios.cameraview.PictureResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView.setLifecycleOwner(this)

        cardView.setOnClickListener {
                cameraView.takePicture()
                progressBar.visibility = View.VISIBLE
        }

        cameraView.addCameraListener(object : CameraListener() {

            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)
                result.toBitmap { bitmap ->
                    extractTextFromImage(FirebaseVisionImage.fromBitmap(bitmap!!)) { text ->
                        extractLanguageFromText(text) { lang ->
                            translateTextToEnglish(lang, text) {
                                progressBar.visibility = View.GONE
                                tvTranslatedText.text = ""
                                tvLang.text = ""
                                tvTranslatedText.text = it
                                tvLang.text = lang
                            }
                        }
                    }
                }
            }
        })

//        cameraView.addFrameProcessor { frame ->
//            extractTextFromImage(getVisionImageFromFrame(frame)) { text ->
//                extractLanguageFromText(text) { lang ->
//                    translateTextToEnglish(lang, text) {
//                        tvTranslatedText.text = ""
//                        tvLang.text = ""
//                        tvTranslatedText.text = it
//                        tvLang.text = lang
//                    }
//                }
//            }
//        }
    }

    private fun translateTextToEnglish(lang: String, text: String, callback: (String) -> Unit) {

        val options = FirebaseTranslatorOptions.Builder()
            .setSourceLanguage(FirebaseTranslateLanguage.languageForLanguageCode(lang) ?: FirebaseTranslateLanguage.EN)
            .setTargetLanguage(FirebaseTranslateLanguage.EN)
            .build()
        val translator = FirebaseNaturalLanguage.getInstance().getTranslator(options)

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener {
                        callback(it)
                    }
                    .addOnFailureListener {
                        callback("Failed to translate")
                        it.printStackTrace()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to download the translation model; please try again ...",
                    Toast.LENGTH_SHORT
                ).show()
                it.printStackTrace()
            }

    }

    private fun extractLanguageFromText(input: String, cb: (String) -> Unit) {
        val languageId = FirebaseNaturalLanguage.getInstance().languageIdentification

        languageId.identifyLanguage(input)
            .addOnSuccessListener {
                Log.e("LANGUAGE", it)
                cb(it)
            }
            .addOnFailureListener {
                cb("en")
                it.printStackTrace()
            }
    }

    private fun extractTextFromImage(image: FirebaseVisionImage, callback: (String) -> Unit) {
        val textDetector = FirebaseVision.getInstance().cloudTextRecognizer

        textDetector.processImage(image)
            .addOnSuccessListener {
                Log.e("TEXT", it.text)
                callback(it.text)
            }
            .addOnFailureListener {
                callback("No text found")
                it.printStackTrace()
            }
    }

    private fun getVisionImageFromFrame(frame: Frame): FirebaseVisionImage {
        //ByteArray for the captured frame`
        val data = frame.data
        //Metadata that gives more information on the image that is to be converted to FirebaseVisionImage
        val imageMetaData = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(FirebaseVisionImageMetadata.ROTATION_90)
            .setHeight(frame.size.height)
            .setWidth(frame.size.width)
            .build()

        return FirebaseVisionImage.fromByteArray(data, imageMetaData)
    }

}
