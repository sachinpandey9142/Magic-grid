package com.magicgrid.game

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.magicgrid.R
import com.magicgrid.ads.AdController
import com.magicgrid.data.GameTelemetry
import com.magicgrid.data.ProgressManager

/**
 * Store screen for acquiring coins via rewarded ads or coin packs.
 */
class StoreActivity : AppCompatActivity() {
    private lateinit var progressManager: ProgressManager
    private lateinit var coinBalanceText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        progressManager = ProgressManager(this)

        val backButton = findViewById<Button>(R.id.btn_store_back)
        val watchAdButton = findViewById<Button>(R.id.btn_store_watch_ad)
        val buySmallButton = findViewById<Button>(R.id.btn_store_buy_small)
        val buyLargeButton = findViewById<Button>(R.id.btn_store_buy_large)
        coinBalanceText = findViewById(R.id.store_coin_text)

        AdController.initialize(this)
        AdController.preloadRewarded(applicationContext, AdController.RewardPlacement.COINS)

        backButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            finish()
        }

        watchAdButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            watchAdForCoins()
        }

        buySmallButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            confirmMoneyPackPurchase(coins = 250, priceLabel = "$1.99")
        }

        buyLargeButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            confirmMoneyPackPurchase(coins = 700, priceLabel = "$4.99")
        }

        refreshCoinBalance()
    }

    private fun refreshCoinBalance() {
        coinBalanceText.text = getString(R.string.store_balance_format, progressManager.getCoins())
    }

    private fun watchAdForCoins() {
        val rewardCoins = 10
        val shown = AdController.showRewardedVideo(
            activity = this,
            placement = AdController.RewardPlacement.COINS,
            onReward = {
                progressManager.addCoins(rewardCoins)
                GameTelemetry.track(this, event = "store_rewarded_coins", detail = "coins=$rewardCoins")
                refreshCoinBalance()
                showToast(getString(R.string.store_watch_ad_reward_format, rewardCoins))
            },
            onFinished = {
                AdController.preloadRewarded(applicationContext, AdController.RewardPlacement.COINS)
            }
        )

        if (!shown) {
            showToast(getString(R.string.store_ad_not_ready))
        }
    }

    private fun confirmMoneyPackPurchase(coins: Int, priceLabel: String) {
        AlertDialog.Builder(this, R.style.DialogAnimation)
            .setTitle(getString(R.string.store_buy_confirm_title))
            .setMessage(getString(R.string.store_buy_confirm_message, coins, priceLabel))
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.store_buy_now) { _, _ ->
                progressManager.addCoins(coins)
                GameTelemetry.track(this, event = "store_coin_pack_purchase", detail = "coins=$coins,price=$priceLabel")
                refreshCoinBalance()
                showToast(getString(R.string.store_purchase_success_format, coins))
            }
            .show()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
