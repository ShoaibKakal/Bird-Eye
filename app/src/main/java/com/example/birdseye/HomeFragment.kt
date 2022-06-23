package com.example.birdseye

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navigationButton = view.findViewById<MaterialButton>(R.id.btn_navigation)
        val textComment = view.findViewById<TextView>(R.id.text_comment)
        navigationButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_mapsFragment)
        }


        textComment.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_feedbackFragment)
        }
    }

}