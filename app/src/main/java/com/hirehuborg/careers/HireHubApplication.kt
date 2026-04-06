package com.hirehuborg.careers

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class HireHubApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox font resources — must run before any PDF extraction
        PDFBoxResourceLoader.init(applicationContext)
    }
}