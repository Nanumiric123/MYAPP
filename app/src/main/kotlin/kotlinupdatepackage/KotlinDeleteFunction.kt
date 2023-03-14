@file: JvmName("KotlinDeleteFunction")
@file:JvmMultifileClass
package kotlinupdatepackage
import android.app.Activity
import android.view.View
import android.widget.Toast
import com.azhon.appupdate.manager.DownloadManager
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

class KotlinDeleteFunction {
    companion object {
        fun update_method1(c: Activity) {

            runBlocking {
            launch {
                val url: URL = URL("http://172.16.206.19/REST_API/debug/output-metadata.json")
                val outputMetadata = url.readText()
                val OMObject = JSONTokener(outputMetadata).nextValue() as JSONObject
                var apkVersion = OMObject.getJSONArray("elements")
                var apkcurrentVersion = apkVersion.getJSONObject(0).getString("versionCode")

                val apkversionName = apkVersion.getJSONObject(0).getString("versionName")

                val manager = DownloadManager.Builder(c).run {
                    apkUrl("http://172.16.206.19/REST_API/debug/app-debug.apk")
                    apkName("app-debug.apk")
                    smallIcon(R.mipmap.ic_launcher)
                    //设置了此参数，那么内部会自动判断是否需要显示更新对话框，否则需要自己判断是否需要更新
                    apkVersionCode(apkcurrentVersion.toInt())
                    //同时下面三个参数也必须要设置
                    apkVersionName(apkversionName)
                    apkSize("16")
                    apkDescription("New Version Available please Update This App!")
                    //省略一些非必须参数...
                    build()
                }
                manager.download()
            }
        }







        }
    }
}