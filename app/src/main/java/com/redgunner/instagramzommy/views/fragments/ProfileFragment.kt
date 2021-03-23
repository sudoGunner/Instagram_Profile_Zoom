package com.redgunner.instagramzommy.views.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

import android.os.Bundle

import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.google.android.gms.ads.*
import com.redgunner.instagramzommy.R
import com.redgunner.instagramzommy.models.profile.AccountResponse
import com.redgunner.instagramzommy.models.search.UserX
import com.redgunner.instagramzommy.utils.showPermissionRequestDialog
import com.redgunner.instagramzommy.viewmodels.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.profile_fragment.*


@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.profile_fragment) {

    private val viewModel: SharedViewModel by activityViewModels()
    private val saveArgs: ProfileFragmentArgs by navArgs()

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var mInterstitialAd: InterstitialAd



    override fun onStart() {
        super.onStart()


        if(viewModel.hasInternetConnection.value==true){

            viewModel.getAccount(saveArgs.userName)


        }else{
            Toast.makeText(this.context,"No internet connection",Toast.LENGTH_LONG).show()

        }

        viewModel.instagramAccount.observe(viewLifecycleOwner, { response ->

            if (response.isSuccessful) {

                val instagramUser = response.body()
                displayItems()

                displayAccount(instagramUser!!)


            } else {
                Toast.makeText(this.context, "Error please try again later", Toast.LENGTH_LONG)
                    .show()

            }

        })

        viewModel.shareItNotify.observe(viewLifecycleOwner,{imagePath ->
            if (imagePath.isNotEmpty()){
                openChooser(imagePath)
               viewModel.shareItNotify.value=""
            }
        })
        viewModel.instagramAccount.value?.body()?.let { displayAccount(it) }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPermissionCallback()
        MobileAds.initialize(this.context) {

        }
        mInterstitialAd = InterstitialAd(this.context)
        mInterstitialAd.adUnitId = "YOUR AD UNIT ID"
        mInterstitialAd.loadAd(AdRequest.Builder().build())


    }


    override fun onResume() {
        super.onResume()

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.favorite -> {
                    viewModel.instagramAccount.value?.body()?.let { addToFavorite(it) }
                    true
                }
                R.id.save -> {

                    if (mInterstitialAd.isLoaded) {
                        mInterstitialAd.show()
                        checkPermissionAndDownloadBitmap()

                    } else {
                        checkPermissionAndDownloadBitmap()

                    }




                    true
                }
                R.id.share -> {



                  viewModel.downloadAndShareIt()

                    true
                }
                else -> false
            }
        }

        topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

    }

    override fun onStop() {
        super.onStop()
        unDisplayItems()

    }


    private fun openChooser(imagePath:String){
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePath))
            type = "image/jpeg"
        }
        startActivity(Intent.createChooser(shareIntent, "Send To"))
    }

    private fun displayAccount(account: AccountResponse) {

        followersCount.text = account.edge_followed_by.count.toString()
        followingCount.text = account.edge_follow.count.toString()
        profileUserName.text = account.full_name
        if (account.is_verified) {
            profileIsCheck.visibility = View.VISIBLE
        }

        Glide.with(this).load(account.profile_pic_url).into(profile_image)

        Glide.with(this).load(account.profile_pic_url).into(profile_image_320)

        Glide.with(this).load(account.profile_pic_url_hd)
            .apply(bitmapTransform(BlurTransformation(25, 3))).into(profile_image_1080)

        profile_image_1080.setOnClickListener {
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
                Glide.with(this).load(account.profile_pic_url_hd).into(profile_image)
                Glide.with(this).load(account.profile_pic_url_hd).into(profile_image_1080)

            }else{
                Glide.with(this).load(account.profile_pic_url_hd).into(profile_image)
                Glide.with(this).load(account.profile_pic_url_hd).into(profile_image_1080)

            }



        }

        profile_image_320.setOnClickListener {
            Glide.with(this).load(account.profile_pic_url).into(profile_image)

        }


    }

    private fun addToFavorite(user: AccountResponse) {
        viewModel.addAccountToFavorite(
            UserX(
                user.full_name, false, user.is_private, user.is_verified, user.profile_pic_url,
                user.username,
                true
            )
        )
    }

    private fun checkPermissionAndDownloadBitmap() {
        when {
            ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {

                saveImageToDevice()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {


                context?.showPermissionRequestDialog(
                    "Permission required",
                    "the application need permission to save image "
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }


    private fun setPermissionCallback() {
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    saveImageToDevice()
                }
            }
    }


    private fun saveImageToDevice() {


        viewModel.saveImage()


    }

    private fun displayItems(){
        textCounter1.visibility=View.VISIBLE
        textCounter2.visibility=View.VISIBLE
        textCounter3.visibility=View.VISIBLE
        textCounter4.visibility=View.VISIBLE
        progressBar.visibility=View.INVISIBLE

    }

    private fun unDisplayItems(){
        textCounter1.visibility=View.INVISIBLE
        textCounter2.visibility=View.INVISIBLE
        textCounter3.visibility=View.INVISIBLE
        textCounter4.visibility=View.INVISIBLE
        progressBar.visibility=View.VISIBLE

    }




}