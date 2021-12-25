package com.example.assistme.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assistme.ObjectAdapter
import com.example.assistme.R
import com.example.assistme.data.Item


/**
 * A simple [Fragment] subclass.
 * Use the [FindFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FindFragment : Fragment() {

    lateinit var recyclerView: RecyclerView
    lateinit var txtSort: TextView
    lateinit var txtFilter: TextView

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
        val view = inflater.inflate(R.layout.fragment_find, container, false)
        txtSort = view.findViewById(R.id.txt_sort)
        txtFilter = view.findViewById(R.id.txt_filter)

        val sortText = SpannableString("  " + requireActivity().resources.getString(R.string.sort))
        val sortImg = resources.getDrawable(R.drawable.ic_sort)
        sortImg.setBounds(0,0,sortImg.intrinsicWidth,sortImg.intrinsicHeight)
        sortImg.setTint(resources.getColor(R.color.colorPrimaryDark))
        val imgSpan = ImageSpan(sortImg)
        sortText.setSpan(imgSpan,0,1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        txtSort.text = sortText

        val filterText = SpannableString("  " + requireActivity().resources.getString(R.string.filter))
        val filterImg = resources.getDrawable(R.drawable.ic_filter)
        filterImg.setBounds(0,0,filterImg.intrinsicWidth,filterImg.intrinsicHeight)
        filterImg.setTint(resources.getColor(R.color.colorPrimaryDark))
        val imSpan = ImageSpan(filterImg)
        filterText.setSpan(imSpan,0,1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        txtFilter.text = filterText

        Log.e("FindFragment", "Inside onCreateView FindFragment")

        val objectList =  ArrayList<Item>()

        objectList.add(Item("Tree", R.drawable.tree))
        objectList.add(Item("Car", R.drawable.car))
        objectList.add(Item("Toothbrush", R.drawable.toothbrush))
        objectList.add(Item("Zebra Crossing", R.drawable.zebra_crossing))
        objectList.add(Item("Chair", R.drawable.chair))
        objectList.add(Item("Water Bottle", R.drawable.water_bottle))
        objectList.add(Item("Glasses", R.drawable.glasses))
        objectList.add(Item("Flowers", R.drawable.flower_pot))
        objectList.add(Item("Laptop", R.drawable.laptop))
        objectList.add(Item("Wheelchair", R.drawable.wheelchair))
        objectList.add(Item("Electronics", R.drawable.electronics))
        objectList.add(Item("Door", R.drawable.door))

        recyclerView = view.findViewById(R.id.recyclerView)

        //TODO: Make the list more eye-pleasing
        val layoutRecycler = GridLayoutManager(context, 2)
        layoutRecycler.offsetChildrenVertical(8)
        layoutRecycler.offsetChildrenHorizontal(8)
        val objectAdapter = ObjectAdapter(context, objectList)

        recyclerView.adapter = objectAdapter
        recyclerView.layoutManager = layoutRecycler

        return view
    }

    override fun onPause() {
        super.onPause()
        Log.e("FindFragment", "onPause FindFragment")
    }

    override fun onStop() {
        super.onStop()
        Log.e("FindFragment", "onStop FindFragment")
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FindFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}