package com.example.lecproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lecproject.databinding.RandomGameBinding
import java.util.Locale

class GameBlockAdapter(private val items: List<Game>) : RecyclerView.Adapter<GameBlockAdapter.ViewHolder>() {
    private var onClickListener: OnClickListener? = null

    inner class ViewHolder(private val binding: RandomGameBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Game) {
            Glide.with(binding.shapeableImageView.context)
                .load(data.background_image)
                .into(binding.shapeableImageView)

            binding.gametitle.text = data.name
            binding.score.text = String.format(Locale.US, "⭐%.1f/5", data.rating)
            binding.developer.text = data.developerName ?: "Loading.."
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RandomGameBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            onClickListener?.onClick(position, items[position])
        }
    }

    fun setOnClickListener(listener: OnClickListener?) {
        this.onClickListener = listener
    }

    interface OnClickListener {
        fun onClick(position: Int, model: Game)
    }
}
