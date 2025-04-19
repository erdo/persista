package foo.bar.example.ui.wallet

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import co.early.fore.core.ui.SyncableView
import co.early.fore.core.logging.Logger
import co.early.fore.core.ui.LifecycleObserver
import foo.bar.example.OG
import foo.bar.example.databinding.ActivityWalletBinding
import foo.bar.example.feature.wallet.Wallet

/**
 * Copyright © 2015-2021 early.co. All rights reserved.
 */
class WalletsActivity : FragmentActivity(), SyncableView {

    private val wallet: Wallet = OG[Wallet::class.java]
    private val logger: Logger = OG[Logger::class.java]

    private lateinit var binding: ActivityWalletBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logger.i("onCreate()")

        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycle.addObserver(LifecycleObserver(this, wallet))

        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        binding.walletIncreaseBtn.setOnClickListener {
            logger.i("increase button clicked")
            wallet.increaseMobileWallet() // observer / reactive ui handles updating the view
        }
        binding.walletDecreaseBtn.setOnClickListener {
            logger.i("decrease button clicked")
            wallet.decreaseMobileWallet() // observer / reactive ui handles updating the view
        }
        binding.walletClearBtn.setOnClickListener {
            logger.i("clear button clicked")
            wallet.resetMobileWallet() // observer / reactive ui handles updating the view
        }
    }

    //reactive UI stuff below
    override fun syncView() {
        logger.i("syncView()")
        binding.walletIncreaseBtn.isEnabled = wallet.state.canIncrease()
        binding.walletDecreaseBtn.isEnabled = wallet.state.canDecrease()
        binding.walletMobileamountTxt.text = wallet.state.mobileWalletAmount.toString()
        binding.walletSavingsamountTxt.text = wallet.state.savingsWalletAmount.toString()
    }
}
