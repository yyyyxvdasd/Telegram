package org.telegram.custom

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.content.edit
import org.telegram.custom.dialog.AppDialogUtils.showCustomDialog
import org.telegram.custom.dialog.CustomCallBack
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.FileLoadOperation
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.DialogsActivity
import org.telegram.ui.LaunchActivity
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * create by ysz on 2023/7/14
 */

const val TAG="TgApp"

@SuppressLint("StaticFieldLeak")
var customBtn: Button? = null
var sDownloadRecView:WeakReference<RecyclerListView> = WeakReference(null)
var dialogAct: DialogsActivity? = null

val sp get() = app.getSharedPreferences("tg_utils",Context.MODE_PRIVATE)
//清理下载任务后剩余的operation
val leftOperations = ConcurrentHashMap<FileLoadOperation,Int>()

@Volatile
var isNeedAddLeft = false

var isAbleLog=false

@JvmOverloads
fun logStackTrace(prefix:String="logStackTrace stack"){
val stack=Log.getStackTraceString(Throwable())
Log.i(TAG,"$prefix=${stack}")
}


private fun Activity.addCustomBtn() {
    customBtn = Button(this).apply {
        alpha = btnProgress.toFloat()/100
        setOnClickListener {
            showCustomDialog()
        }
    }
    addCustomView(customBtn!!) {
        width = 150.dp
        height = 80.dp
        marginStart = 80.dp
    }
}

fun View?.showCustomDialog(){
    asActivity().showCustomDialog()
}

 fun Activity?.showCustomDialog(){
     this?:return
    showCustomDialog(this,object : CustomCallBack {
        override fun editCustomBtn() {
            if (customBtn == null) {
                addCustomBtn()
            } else {
                removeCustomBtn()
            }
            sp.edit {
                putBoolean(KEY_ADD_CUSTOM, customBtn != null)
            }
        }

        override fun onSwitchTimeChange(switchTime: Int) {
            sp.edit {
                putInt(KEY_SWITCH_TIME, switchTime)
            }
        }

        override fun onSwitchCountChange(switchCount: Int) {
            sp.edit {
                putInt(KEY_SWITCH_COUNT, switchCount)
            }
        }

        override fun cancelDownload() {
           cancelAllDownload()
        }

        override fun selectMoreDownload() {
            val recView= sDownloadRecView.get()?:return
            recView.selectFirstMore()
        }

        override fun enterDownloadAct() {
            val fragment=LaunchActivity.getLastFragment()
            Log.i(TAG,"enterDownloadAct current fragment=$fragment")
            if(fragment is DialogsActivity){
                fragment.actionBar.actionBarMenuOnItemClick.onItemClick(3)
            }else{
                val dialogsActivity=DialogsActivity(null)
                fragment.presentFragment(dialogsActivity)
                dialogsActivity.showSearch(true, true, true)
//                dialogsActivity.actionBar.openSearchField(true)
            }
        }

        override fun changeBtnAlpha(progress: Int) {
            sp.edit {
                putInt(KEY_BTN_PROGRESS, progress)
            }
            customBtn?.apply {
                alpha = progress.toFloat() / 100
            }
        }

        override fun onMaxDownloadRequestChange(count: Int) {
            sp.edit {
                putInt(KEY_MAX_DOWNLOAD_REQUEST, count)
            }
        }
    })
}

fun cancelAllDownload(){
    val accountInstance= getAccountInstance()
    Log.i(TAG,"cancelDownload accountInstance =${accountInstance}")
    accountInstance?:return
    val fileLoader=accountInstance.fileLoader
    val loadOperationPathsUI = fileLoader.loadOperationPathsUI
    Log.i(TAG, "cancel loadOperationPathsUI count=${loadOperationPathsUI.size}")
    val loadOperationPaths=fileLoader.loadOperationPaths
    for ((k,v) in loadOperationPaths){
        val count=v.queue.count
        Log.i(TAG,"cancel loading filename=${k} .quene count=${count}")

    }
    //清除所有下载队列
    accountInstance.messagesStorage.clearDownloadQueue(0)
    runUi(150) {
        isNeedAddLeft=true
        accountInstance.messagesStorage.clearDownloadQueue(0)
        runUi(500) {
            runSafe {
                accountInstance.messagesStorage.clearDownloadQueue(0)
                isNeedAddLeft=false
                accountInstance.messagesStorage.clearDownloadQueue(0)
                accountInstance.fileRefController.clear()
                if (leftOperations.size <= 0) {
                    return@runSafe
                }
                showToast("清理剩余下载个数为${leftOperations.size}")
                leftOperations.keys.forEach {
                    it.queue.cancel(it)
                    Log.i(TAG,"clearOperaion operation=${it}")
                    it.clearOperation(null, false, true)
                    it.cancel()
                }
                leftOperations.clear()
                accountInstance.messagesStorage.clearDownloadQueue(0)
                isAbleLog=true
            }
        }
    }
    showToast("已取消所有下载个数为${fileLoader.cancelLoadAllFilesCustom()}")
}

/**
 * 设置点击切换快进方式
 */
fun View.setCustomSwitch(){
    setOnClickListener {
        val isNormal= !switchNormal
        sp.edit {
            putBoolean(KEY_IS_SWITCH_NORMAL, isNormal)
        }
        showToast("切换快进方式：${if (isNormal) "默认" else "分片"}")
    }
}

private fun Activity.removeCustomBtn() {
    runSafe {
        removeCustomView(customBtn)
    }
    customBtn = null
}

private const val KEY_ADD_CUSTOM="is_add_custom"
private const val KEY_SWITCH_TIME="switch_time"
private const val KEY_BTN_PROGRESS="btn_progress"
//是否正常快进，否的话采用分片方式快进
private const val KEY_IS_SWITCH_NORMAL = "is_switch_normal"
//分片片段数
private const val KEY_SWITCH_COUNT = "switch_count"
private const val KEY_MAX_DOWNLOAD_REQUEST="max_download_request"

val switchTimeLong get() = sp.getInt(KEY_SWITCH_TIME,10)*1000
val switchTimeSec get() = sp.getInt(KEY_SWITCH_TIME,10)
val switchNormal get() = sp.getBoolean(KEY_IS_SWITCH_NORMAL, true)
val switchCount get() = sp.getInt(KEY_SWITCH_COUNT, 60).coerceAtLeast(2)
val btnProgress get() = sp.getInt(KEY_BTN_PROGRESS,10)
val maxDownloadRequest get() = sp.getInt(KEY_MAX_DOWNLOAD_REQUEST, -1)

fun Activity.checkAddCustomBtn() {
    runSafe {
        if (sp.getBoolean(KEY_ADD_CUSTOM, false)) {
            addCustomBtn()
        }
    }
}

fun getAccountInstance(): AccountInstance? {
    return AccountInstance.getInstance(UserConfig.selectedAccount)
}
val <T> T.weakRef: WeakReference<T>
    get() = WeakReference(this)

//归档
fun BaseFragment.archive(dialogId:Long){
    messagesController.addDialogToFolder(arrayListOf(dialogId), 1, -1, null, 0)
    showToast("归档成功")
}

