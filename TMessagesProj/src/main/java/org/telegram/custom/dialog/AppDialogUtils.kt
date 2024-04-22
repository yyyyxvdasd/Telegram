package org.telegram.custom.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import org.telegram.custom.*
import org.telegram.messenger.FileLoadOperation
import org.telegram.messenger.databinding.DialogCustomBinding

/**
 * create by ysz on 2023/7/14
 */
object AppDialogUtils {
    
    fun showCustomDialog(context: Context, callBack: CustomCallBack){
        runSafe {
            val binding = DialogCustomBinding.inflate(LayoutInflater.from(context))
            binding.btnEditCustom.text = "${if (customBtn != null) "移除" else "增加"}按钮"
            val dialog = AlertDialog.Builder(context).setView(binding.root).setCancelable(true)
                .show()
            binding.btnEditCustom.setOnClickListener {
                runSafe {
                    callBack.editCustomBtn()
                    dialog.dismiss()
                }
            }
            binding.etSwitchTime.setText("$switchTimeSec")
            binding.etSwitchTime.addTextChangedListener(afterTextChanged = {
                val time=it?.toString()?.toIntOrNull()?:return@addTextChangedListener
                callBack.onSwitchTimeChange(time)
            })
            binding.etSwitchCount.setText("$switchCount")
            binding.etSwitchCount.addTextChangedListener(afterTextChanged = {
                val count=it?.toString()?.toIntOrNull()?:return@addTextChangedListener
                callBack.onSwitchCountChange(count)
            })
            binding.tvCurrentRequestCount.text = "当前并行下载数：${FileLoadOperation.currentMaxRequestCount}"
            binding.etMaxDownloadRequest.setText("$maxDownloadRequest")
            binding.etMaxDownloadRequest.addTextChangedListener(afterTextChanged = {
                val count=it?.toString()?.toIntOrNull()?:return@addTextChangedListener
                callBack.onMaxDownloadRequestChange(count)
            })
            binding.btnCancelDownload.setOnClickListener {
                runSafe {
                    callBack.cancelDownload()
                }
            }
            binding.btnSelectMore.setOnClickListener {
                callBack.selectMoreDownload()
            }
            binding.btnEnterDownload.setOnClickListener {
                callBack.enterDownloadAct()
                dialog.dismiss()
            }
            binding.seekBar.progress = btnProgress
            binding.seekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    callBack.changeBtnAlpha(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })
        }
    }
}
interface CustomCallBack{
    fun editCustomBtn()
    fun onSwitchTimeChange(switchTime:Int)
    fun onSwitchCountChange(switchCount:Int)
    fun cancelDownload()
    fun selectMoreDownload()
    fun enterDownloadAct()
    fun changeBtnAlpha(progress:Int)
    fun onMaxDownloadRequestChange(count:Int)
}