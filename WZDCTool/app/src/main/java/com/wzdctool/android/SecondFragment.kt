package com.wzdctool.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.wzdctool.android.dataclasses.SecondFragmentUIObj
import kotlin.math.min

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

//    companion object {
//        fun newInstance() = test_fragment()
//    }

    private lateinit var viewModel: SecondFragmentViewModel
    private lateinit var uiObjObserver: Observer<SecondFragmentUIObj>
    private lateinit var localUIObj: SecondFragmentUIObj

    val buttons = listOf(0, R.id.lane1btn, R.id.lane2btn, R.id.lane3btn, R.id.lane4btn, R.id.lane5btn, R.id.lane6btn, R.id.lane7btn, R.id.lane8btn)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ititializeLaneBtns()
    }



//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
////        view.findViewById<Button>(R.id.button_second).setOnClickListener {
////            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
////        }
//    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SecondFragmentViewModel::class.java)
        // TODO: Use the ViewModel
        // viewModel.numLanes
        uiObjObserver = Observer<SecondFragmentUIObj> {
            localUIObj = it
            updateUI()
        }
        viewModel.currentUIObj.observe(viewLifecycleOwner, uiObjObserver)
    }

    private fun updateUI() {

    }

    private fun laneClicked(lane: Int) {

    }

    private fun ititializeLaneBtns(numLanes: Int) {
        val btn1params = requireView().findViewById<ToggleButton>(R.id.lane1btn).layoutParams as ConstraintLayout.LayoutParams
        if (numLanes <= 1 ) {
            // TODO: Throw exception
        }
        else if (numLanes > 1) {
            btn1params.endToStart = R.id.lane2btn
            btn1params.startToStart = 0

            for (i in 2..min(numLanes, 4)) {
                val button = requireView().findViewById<ToggleButton>(buttons[i])
                val params = button.layoutParams as ConstraintLayout.LayoutParams
                params.startToEnd = buttons[i-1]
                if (i == 4) params.endToEnd = 0
                else params.endToEnd = buttons[i+1]
                button.visibility = View.VISIBLE
                button.setOnClickListener {
                    laneClicked(i)
                }
            }

            // Second row of lane buttons
            if (numLanes == 5) {
                val btn5params = requireView().findViewById<ToggleButton>(R.id.lane5btn).layoutParams as ConstraintLayout.LayoutParams
                btn1params.endToEnd = 0
                btn1params.startToStart = 0
                requireView().findViewById<ToggleButton>(R.id.lane5btn).visibility = View.VISIBLE
            }
            else if (numLanes > 5) {
                val btn5params = requireView().findViewById<ToggleButton>(R.id.lane5btn).layoutParams as ConstraintLayout.LayoutParams
                btn5params.endToStart = R.id.lane6btn
                btn5params.startToStart = 0
                requireView().findViewById<ToggleButton>(R.id.lane5btn).visibility = View.VISIBLE
                for (i in 6..numLanes) {
                    val button = requireView().findViewById<ToggleButton>(buttons[i])
                    val params = button.layoutParams as ConstraintLayout.LayoutParams
                    params.startToEnd = buttons[i-1]
                    if (i == numLanes) params.endToEnd = 0
                    else params.endToEnd = buttons[i+1]
                    button.visibility = View.VISIBLE
                    button.setOnClickListener {
                        laneClicked(i)
                    }
                }
            }
        }
    }
}