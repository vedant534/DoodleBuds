package com.example.doodlebuds

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DrawingView(context:Context,attrs:AttributeSet): View(context, attrs) {

    private var mDrawPath : CustomPath? =null
    private var mCanvasBitmap:Bitmap? =null

    internal inner class CustomPath(
        var color: Int,
        var brushThickness:Float ) : Path()
    {



    }


}