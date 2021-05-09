package foo.bar.example.ui.wallet

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import co.early.fore.core.ui.SyncableView
import co.early.fore.kt.core.logging.Logger
import co.early.fore.kt.core.ui.ForeLifecycleObserver
import foo.bar.example.OG
import foo.bar.example.R
import foo.bar.example.feature.wallet.Wallet
import kotlinx.android.synthetic.main.activity_wallet.*

/**
 * Copyright © 2015-2021 early.co. All rights reserved.
 */
@ExperimentalStdlibApi
class WalletsActivity : FragmentActivity(R.layout.activity_wallet), SyncableView {

    private val wallet: Wallet = OG[Wallet::class.java]
    private val logger: Logger = OG[Logger::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.i("onCreate()")

        lifecycle.addObserver(ForeLifecycleObserver(this, wallet))

        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        wallet_increase_btn.setOnClickListener {
            logger.i("increase button clicked")
            wallet.increaseMobileWallet() // observer / reactive ui handles updating the view
        }
        wallet_decrease_btn.setOnClickListener {
            logger.i("decrease button clicked")
            wallet.decreaseMobileWallet() // observer / reactive ui handles updating the view
        }
    }

    //reactive UI stuff below
    override fun syncView() {
        logger.i("syncView()")
        wallet_increase_btn.isEnabled = wallet.state.canIncrease()
        wallet_decrease_btn.isEnabled = wallet.state.canDecrease()
        wallet_mobileamount_txt.text = wallet.state.mobileWalletAmount.toString()
        wallet_savingsamount_txt.text = wallet.state.savingsWalletAmount.toString()
    }
}
