package com.muse.pdfrendererdemo

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.muse.pdfrendererdemo.adapter.PDFAdapter
import com.permissionx.guolindev.PermissionX
import java.io.*


class MainActivity : AppCompatActivity() {
    private lateinit var rvList: RecyclerView
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private val mHandOutList: MutableList<Bitmap> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvList = findViewById(R.id.rvList)
        getPermissions(this)
    }

    private fun showPdf() {
        //调用方法
        val dirPath = ContextCompat.getExternalFilesDirs(
            this@MainActivity,
            Environment.DIRECTORY_DCIM
        )[0].absolutePath + File.separator.toString() + "PrintFile"
        try {
            copyAssetToFile("vue.pdf", dirPath, "/测试vue.pdf")
        } catch (e: IOException) {
            LogUtils.e(e)
            e.printStackTrace()
        }
//读取到本地file文件
        //读取到本地file文件
        val file = File("$dirPath/测试vue.pdf")
        ThreadUtils.runOnUiThread {
            parcelFileDescriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            parcelFileDescriptor?.apply {
                pdfRenderer = PdfRenderer(this)
                pdfRenderer?.apply {
                    for (i in 0 until this.pageCount) {
                        mHandOutList.add(renderPage(this.openPage(i)))
                    }
                    if (this@MainActivity::rvList.isInitialized) {
                        rvList.apply {
                            layoutManager = LinearLayoutManager(this@MainActivity)
                            adapter = PDFAdapter(this@MainActivity).apply {
                                setData(mHandOutList)
                            }
                        }
                    }
                }

            }
        }

    }

    private fun renderPage(page: PdfRenderer.Page): Bitmap {
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
    }

    /**
     * 复制assets下的文件到本地文件
     * assetName assets内的文件名称
     * savepath 本地文件夹路径
     * savename 保存的文件名称需带后缀文件类型 如.pdf
     * @throws IOException
     */
    @Throws(IOException::class)
    fun copyAssetToFile(assetName: String?, savepath: String, savename: String) {
        val dir = File(savepath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val dbf = File(savepath + savename)
        if (dbf.exists()) {
            dbf.delete()
        }
        val outFileName = savepath + savename
        val myOutput: OutputStream = FileOutputStream(outFileName)
        val myInput: InputStream = this.assets.open(assetName!!)
        val buffer = ByteArray(1024)
        var length: Int
        while (myInput.read(buffer).also { length = it } > 0) {
            myOutput.write(buffer, 0, length)
        }
        myOutput.flush()
        myInput.close()
        myOutput.close()
    }

    private fun getPermissions(activity: FragmentActivity) {
        PermissionX.init(activity)
            .permissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
            .explainReasonBeforeRequest()
            .request { allGranted: Boolean, grantedList: List<String?>, deniedList: List<String?>? ->
                showPdf()
            }
    }
}