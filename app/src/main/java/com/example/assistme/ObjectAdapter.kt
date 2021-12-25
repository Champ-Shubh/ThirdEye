package com.example.assistme

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assistme.data.Item

class ObjectAdapter(var context: Context?, var itemList: List<Item>): RecyclerView.Adapter<ObjectAdapter.ObjectViewHolder>() {

    class ObjectViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imgItem:ImageView = itemView.findViewById(R.id.img_item)
        val txtItemName: TextView = itemView.findViewById(R.id.txt_item_name)
        val containerView: CardView = itemView.findViewById(R.id.container_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_single_item, parent, false)
        return ObjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObjectViewHolder, position: Int) {
        val item = itemList[position]

        holder.txtItemName.text = item.itemName

        //Glide.with(context).load(ContextCompat.getDrawable(context!!,item.itemPicID)).into(holder.imgItem)
        holder.imgItem.setImageResource(item.itemPicID)

        holder.containerView.setOnClickListener {
            val searchIntent = Intent(context, ItemFindActivity::class.java)
            searchIntent.putExtra("itemName", item.itemName)
            searchIntent.putExtra("itemPicID", item.itemPicID)

            context?.startActivity(searchIntent)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}