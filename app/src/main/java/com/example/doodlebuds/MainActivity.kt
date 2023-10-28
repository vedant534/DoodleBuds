package com.example.doodlebuds

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var mImgBtnCurrPaint: ImageButton? = null
    private var customProgressDialog : Dialog?= null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result->
            if(result.resultCode == RESULT_OK && result.data!=null){
                //assign this result to our img view
                val imageBackground: ImageView = findViewById(R.id.iv_bg)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permission->
            permission.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted){
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now access storage files",
                        Toast.LENGTH_LONG
                    ).show()
                    //go and select image
                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                else{
                    if(permissionName==Manifest.permission.READ_MEDIA_IMAGES){
                        Toast.makeText(
                            this@MainActivity,
                            "Oops you just denied the permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(10.toFloat())

        val ibColor : ImageButton = findViewById(R.id.ib_color)
        ibColor.setOnClickListener{
            showColors()
        }

        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener{
            drawingView?.onClickUndo()
        }

        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener{
            drawingView?.onClickRedo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener{
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_dv_container)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }

        }

    }

    private fun showColors() {
        val colorLayout: LinearLayout = findViewById(R.id.ll_paint_colors)
        if(colorLayout.visibility == View.VISIBLE) colorLayout.visibility = View.INVISIBLE
        else colorLayout.visibility = View.VISIBLE
    }

    private fun showRD(
        title: String,
        message: String
    ){
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)

        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog,_->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun isReadStorageAllowed(): Boolean{
        //also gives access to write in newer android versions
        val result = ContextCompat.checkSelfPermission(
                this,Manifest.permission.READ_MEDIA_IMAGES)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if(
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_MEDIA_IMAGES)
            ){
            showRD("DoodleBuds", "needs access to external storage to import")
        }
        else{
            requestPermission.launch(
                arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
            )
        }

    }

    private fun showBrushSizeChooserDialog() {

        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size :")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val medBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        medBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked( view: View){
        if(view !== mImgBtnCurrPaint){
            val imgBtn = view as ImageButton
            val colorTag = imgBtn.tag.toString()
            drawingView?.setColor(colorTag)

            imgBtn.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )

            mImgBtnCurrPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mImgBtnCurrPaint = view
        }
        else {

            view.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            mImgBtnCurrPaint = findViewById(R.id.blacky)
            val colorTag = findViewById<ImageButton>(R.id.blacky).tag.toString()
            drawingView?.setColor(colorTag)

            mImgBtnCurrPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

        }
    }

    private fun getBitmapFromView(view : View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile( mBitmap: Bitmap): String {
        var result =""
        withContext(Dispatchers.IO){
            try {
                val bytes = ByteArrayOutputStream()
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
                val f = File( externalCacheDir?.absoluteFile.toString()
                        + File.separator
                        + "DoodleBuds"
                        + System.currentTimeMillis()/1000
                        + ".jpeg")

                val fo = FileOutputStream(f)
                fo.write(bytes.toByteArray())
                fo.close()
                result = f.absolutePath


                runOnUiThread{
                    cancelDialog()
                    if(result.isNotEmpty()){
                        Toast.makeText(this@MainActivity, externalCacheDir?.absoluteFile.toString(), Toast.LENGTH_SHORT).show()
                        shareImage(f)
                    }
                    else{
                        Toast.makeText(this@MainActivity, "something went wrong, while saving the file", Toast.LENGTH_SHORT).show()
                    }
                }
            }catch(e: Exception){
                result = ""
                e.printStackTrace()
            }
        }
        return result
    }


    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelDialog(){
        if(customProgressDialog!=null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(mFile:File){
        setUpEnablingFeatures(FileProvider.getUriForFile(this@MainActivity,"com.example.doodlebuds.fileprovider",mFile))
    }

    private fun setUpEnablingFeatures(uri:Uri){
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.type = "image/jpeg"
        startActivity(Intent.createChooser(intent, "Share image via "))
    }
}