package com.odman.project_mole

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.odman.project_mole.ml.LiteModel
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : AppCompatActivity() {

    lateinit var btnSec:Button
    lateinit var btmp:Bitmap

    private fun model(bitmap: Bitmap?){

        // Görüntüyü modele uygun olacak şekilde işleme
        val imageProcessor= ImageProcessor.Builder()
            .add(ResizeOp(224,224,ResizeOp.ResizeMethod.BILINEAR))
            .build()

        // Tensör görüntünün veri tipini ayarlama
        var tensorImage= TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage=imageProcessor.process((tensorImage))

        // Modeli çağırma
        val model = LiteModel.newInstance(applicationContext)

        // Görüntüyü işleme ve veri tipini ayarlama
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(tensorImage.buffer)

        // Tahmin yapma ve çıktının veri tipini ayarlama
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

        // Çıktıya göre arayüze sonucu yazdırma
        var largest=outputFeature0[0]
        var count=0
        var result=0
        for(num in outputFeature0){
            count=count+1
            if(largest < num)
            {
                largest=num
                result=count
            }
        }

        if(result==1)textView.text= "İyi huylu"
        else textView.text= "Kötü huylu"

        model.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSec = findViewById(R.id.buttonSec)

        // Cihaz deposundan işlenecek veriyi seçme
        btnSec.setOnClickListener{
            val intent=Intent()
            intent.setAction((Intent.ACTION_GET_CONTENT))
            intent.setType("image/*")
            startActivityForResult(intent,102)
        }

        button.isEnabled=false

        // Kamera kullanımı için izin işlemi
        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
          ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),111)
        }
        else{
            button.isEnabled=true
        }

        // Kamera ile görüntü kaydetme
        button.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 101)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Kamera ile kaydedilen görüntünün tahmin için modele gönderilmesi ve arayüzde görüntülenmesi
        if (requestCode == 101 && resultCode == RESULT_OK){
            val picture=data?.getParcelableExtra<Bitmap>("data")
            imageView.setImageBitmap(picture)

            model(picture)
        }

        // Cihaz deposundan görüntü seçilerek tahmin için modele gönderilmesi ve arayüzde görüntülenmesi
        if(requestCode==102 && resultCode == RESULT_OK){
            val uri=data?.data
            btmp=MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
            imageView.setImageBitmap(btmp)

            model(btmp)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Kamera ile görüntü kaydetme izninin kontrolü
        if (requestCode == 111 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            button.isEnabled=true
        }
    }
}