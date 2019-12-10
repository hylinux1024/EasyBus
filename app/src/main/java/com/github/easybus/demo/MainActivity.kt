package com.github.easybus.demo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.easybus.EasyBus
import com.github.easybus.Logger
import com.github.easybus.MyEventBusIndex
import com.github.easybus.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
//        EasyBus.getInstance().register(this)
        EventBus.builder().addIndex(MyEventBusIndex()).installDefaultEventBus()
        EventBus.getDefault().register(this)
        fab.setOnClickListener { view ->
            //            EasyBus.getInstance().post(MessageEvent("message from action"))
            EventBus.getDefault().post(MessageEvent("message from action"))
//            Snackbar.make(view, "post as message", Snackbar.LENGTH_LONG)
//                    .setAction("Action") {
//
//                    }.show()
        }

        registerBtn.setOnClickListener {
            EasyBus.getInstance().register(this)
            Toast.makeText(this, "Register EasyBus", Toast.LENGTH_SHORT).show()
        }
        unregisterBtn.setOnClickListener {
            EasyBus.getInstance().unregister(this)
            Toast.makeText(this, "Unregister EasyBus", Toast.LENGTH_SHORT).show()
        }

        easyPostBtn.setOnClickListener {
            EasyBus.getInstance().post(MessageEvent("Message post from easy bus"))
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventUpdate(event: MessageEvent) {
        Logger.i("onEventUpdate:" + event.message)
        Toast.makeText(this, "onEventUpdate: ${event.message}", Toast.LENGTH_SHORT).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventUpdate2(event: MessageEvent) {
        Logger.i("onEventUpdate2: " + event.message)
        Toast.makeText(this, "onEventUpdate2: ${event.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onDetachedFromWindow() {
//        EasyBus.getInstance().unregister(this)
        EventBus.getDefault().unregister(this)
        super.onDetachedFromWindow()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
