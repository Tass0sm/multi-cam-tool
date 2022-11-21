package com.tassosm.multicamtool

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.tassosm.multicamtool.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Helper class used to encapsulate a logical camera and two underlying
     * physical cameras
     */
    data class DualCamera(val logicalId: String, val physicalId1: String, val physicalId2: String)

    // From google.
    private fun findDualCameras(manager: CameraManager, facing: Int? = null): List<DualCamera> {
        val dualCameras = ArrayList<DualCamera>()

        // Iterate over all the available camera characteristics
        manager.cameraIdList.map {
            Pair(manager.getCameraCharacteristics(it), it)
        }.filter {
            // Filter by cameras facing the requested direction
            facing == null || it.first.get(CameraCharacteristics.LENS_FACING) == facing
        }.filter {
            // Filter by logical cameras
            // CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA requires API >= 28
            it.first.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
        }.forEach {
            // All possible pairs from the list of physical cameras are valid results
            // NOTE: There could be N physical cameras as part of a logical camera grouping
            // getPhysicalCameraIds() requires API >= 28
            val physicalCameras = it.first.physicalCameraIds.toTypedArray()
            for (idx1 in physicalCameras.indices) {
                for (idx2 in (idx1 + 1) until physicalCameras.size) {
                    dualCameras.add(DualCamera(
                        it.second, physicalCameras[idx1], physicalCameras[idx2]))
                }
            }
        }

        return dualCameras
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val dualCams = findDualCameras(cameraManager)

        // create an array adapter and pass the required parameter
        // in our case pass the context, drop down layout , and array.
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, dualCams)
        // get reference to the autocomplete text view
        // set adapter to the autocomplete tv to the arrayAdapter
        binding.autoCompleteTextView.setAdapter(arrayAdapter)
    }
}