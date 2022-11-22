/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tassosm.multicamtool.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tassosm.multicamtool.DualCamera
import com.tassosm.multicamtool.GenericListAdapter
import com.tassosm.multicamtool.R

class SelectorFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = RecyclerView(requireContext())

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view as RecyclerView

        view.apply {
            layoutManager = LinearLayoutManager(requireContext())

            val cameraManager =
                    requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val dualCameraList = enumerateDualCameras(cameraManager)
            // instead of: val cameraList = enumerateCameras(cameraManager)

            val layoutId = android.R.layout.simple_list_item_1

            adapter = GenericListAdapter(dualCameraList, itemLayoutId = layoutId) { view, item, _ ->
                view.findViewById<TextView>(android.R.id.text1).text = item.title
                view.setOnClickListener {
                    Navigation.findNavController(requireActivity(), R.id.fragment_container)
                        .navigate(SelectorFragmentDirections.actionSelectorToCamera(
                            item.dualCamera, item.format))
                }
            }
        }

    }

    companion object {

        /** Helper class used as a data holder for each selectable camera format item */
        private data class FormatItem(val title: String, val dualCamera: DualCamera, val format: Int)

        /** Helper function used to convert a lens orientation enum into a human-readable string */
        private fun lensOrientationString(value: Int) = when(value) {
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        /** Helper function used to list all compatible cameras and supported pixel formats */
        @SuppressLint("InlinedApi")
        private fun enumerateDualCameras(cameraManager: CameraManager): List<FormatItem> {
            val availableDualCameras: MutableList<DualCamera> = mutableListOf()
            val listItems: MutableList<FormatItem> = mutableListOf()

            // Get list of all compatible cameras
            val cameraIds = cameraManager.cameraIdList.filter {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ?: false
            }


            // Iterate over all the available camera characteristics
            cameraIds.map {
                Pair(cameraManager.getCameraCharacteristics(it), it)
            }.filter {
                // Filter by logical cameras
                it.first.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
            }.forEach {
                // All possible pairs from the list of physical cameras are valid results
                // NOTE: There could be N physical cameras as part of a logical camera grouping
                val physicalCameras = it.first.physicalCameraIds.toTypedArray()
                for (idx1 in 0 until physicalCameras.size) {
                    for (idx2 in (idx1 + 1) until physicalCameras.size) {
                        availableDualCameras.add(DualCamera(
                            it.second, physicalCameras[idx1], physicalCameras[idx2]))
                    }
                }
            }

            // Iterate over the list of cameras and return all the compatible ones
            availableDualCameras.forEach { dualCam ->
                val characteristics = cameraManager.getCameraCharacteristics(dualCam.logicalId)
                val orientation = lensOrientationString(
                    characteristics.get(CameraCharacteristics.LENS_FACING)!!)

                // Query the available capabilities and output formats
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!
                val outputFormats = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats

                // All cameras *must* support JPEG output so we don't need to check characteristics
                listItems.add(FormatItem(
                    "$orientation JPEG ($dualCam)", dualCam, ImageFormat.JPEG))

                // Return cameras that support RAW capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) &&
                    outputFormats.contains(ImageFormat.RAW_SENSOR)) {
                    listItems.add(FormatItem(
                        "$orientation RAW ($dualCam)", dualCam, ImageFormat.RAW_SENSOR))
                }

                // Return cameras that support JPEG DEPTH capability
                if (capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) &&
                    outputFormats.contains(ImageFormat.DEPTH_JPEG)) {
                    listItems.add(FormatItem(
                        "$orientation DEPTH ($dualCam)", dualCam, ImageFormat.DEPTH_JPEG))
                }
            }

            return listItems
        }
    }
}
