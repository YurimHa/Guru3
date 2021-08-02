package com.example.backbone

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.backbone.databinding.*
import org.jsoup.Jsoup
import java.io.BufferedInputStream
import java.net.URL
import java.net.URLConnection

private var isrun:Boolean = false
class ReadMultiAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>()  {
    private lateinit var binding: ReadQuestionItemBinding
    private lateinit var binding2: ReadContentItemBinding

    private val items = mutableListOf<ReadItem>()

    companion object {
        private const val TYPE_Question = 0
        private const val TYPE_Content = 1
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ReadQuestionData -> {
            TYPE_Question
        }
        is ReadContentData -> {
            TYPE_Content
        }
        else -> {
            throw IllegalStateException("Not Found ViewHolder Type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        TYPE_Question -> {
            MyQHolder.create(parent)
        }
        TYPE_Content -> {
            MyContentHolder.create(parent)
        }
        else -> {
            throw IllegalStateException("Not Found ViewHolder Type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyQHolder -> {
                holder.setQList(items[position] as ReadQuestionData)
            }
            is MyContentHolder -> {
                holder.setContentList(items[position] as ReadContentData)
            }
        }
    }

    // 질문 Holder
    class MyQHolder(val binding: ReadQuestionItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun setQList(item: ReadQuestionData) {
            // 질문 제목
            if(item.qTitle == ""){
                binding.qTitle.visibility = View.GONE
            }else{
                binding.qTitle.text = item.qTitle
            }

            if(item.aImg != null)
            {
                //*****나중에 구현
                // 삽입 이미지
                //binding.aImg.setImageDrawable(item.aImg)
            }else{
                binding.aImg.visibility = View.GONE
            }


            // 링크
            if(item.linkUri == ""||item.linkUri == null){
                //링크 내용이 없으면?
                binding.clLinkArea.visibility = View.GONE
            }else{
                //링크 내용이 있으면?
                //binding.clLinkArea.visibility = item.linkLayout?.visibility!!
                    loadLink(item.linkUri.toString())
            }

            // 대답
            binding.aTxt.text = item.aTxt

        }

        private fun setLink(linkUri: String, title: String, content:String, bm1:Bitmap?)
        {
            binding.linkUri.text = linkUri
            binding.linkTitle.text = title
            binding.linkContent.text = content
            binding.linkIcon.setImageBitmap(bm1)
        }

        private fun loadLink(linkUri: String){
            // 링크 삽입 관련 메소드
            var title: String = ""
            var bm1: Bitmap? = null
            var url1: URL? = null
            var content:String = ""
            isrun = true
            Thread(Runnable {
                while(isrun)
                {//네이버의 경우에만 해당되는 것 같아.
                    try{
                        if (linkUri.contains("naver")) {
                            //linkIcon에 파비콘 추출해서 삽입하기
                            val doc = Jsoup.connect("${linkUri}").get()

                            //제목 들고 오기
                            val link2 = doc.select("body").select("iframe[id=mainFrame]").attr("src")//.attr("content")
                            if(linkUri.contains("blog"))
                            {
                                val doc2 = Jsoup.connect("https://blog.naver.com/${link2}").get()
                                title = doc2.title()
                                content = doc2.select("meta[property=\"og:description\"]").attr("content")
                            }else if(linkUri == "https://www.naver.com/"){
                                title = doc.title()
                                content = doc.select("meta[name=\"og:description\"]").attr("content")
                            }else{
                                title = doc.title()
                                content = doc.select("meta[property=\"og:description\"]").attr("content")
                            }
                            url1 = URL("https://ssl.pstatic.net/sstatic/search/favicon/favicon_191118_pc.ico")
                            var conn: URLConnection = url1!!.openConnection()
                            conn.connect()
                            var bis: BufferedInputStream = BufferedInputStream(conn.getInputStream())
                            bm1 = BitmapFactory.decodeStream(bis)
                            bis.close()
                            setLink(linkUri.toString(), title.toString(), content.toString(), bm1)

                            isrun=false
                        } else {

                            val doc = Jsoup.connect("${linkUri}").get()
                            var favicon:String
                            var link:String
                            if(linkUri.contains("google"))
                            {
                                favicon = doc.select("meta[itemprop=\"image\"]").attr("content")
                                link = "https://www.google.com"+favicon
                                url1 = URL("${link}")
                            }else{
                                //파비콘 이미지 들고 오기
                                favicon = doc.select("link[rel=\"icon\"]").attr("href")
                                if(favicon=="")
                                {
                                    favicon = doc.select("link[rel=\"SHORTCUT ICON\"]").attr("href")
                                }
                                if (!favicon.contains("https:")) {
                                    link = "https://"+favicon
                                    url1 = URL("${link}")
                                }else{
                                    url1 = URL("${favicon}")
                                }
                            }

                            try{
                                var conn: URLConnection = url1!!.openConnection()
                                conn.connect()
                                var bis: BufferedInputStream = BufferedInputStream(conn.getInputStream())
                                bm1 = BitmapFactory.decodeStream(bis)
                                bis.close()
                            }catch (e:Exception)
                            {
                            }
                            title = doc.title()

                            content = doc.select("meta[name=\"description\"]").attr("content")
                            if(content == "")
                            {
                                content = doc.select("meta[property=\"og:site_name\"]").attr("content")
                            }
                            if(title == "")
                            {
                                title = doc.select("meta[property=\"og:site_name\"]").attr("content")
                            }
                            if(bm1==null)
                            {
                                binding.linkIcon.visibility= View.GONE
                            }
                            setLink(linkUri, title, content, bm1)
                            isrun=false
                        }
                    }catch(e:Exception){
                        //링크가 올바르지 않을때->안내 토스트 메시지를 띄움

                    }
                }

            }).start()
        }

        companion object Factory {
            fun create(parent: ViewGroup): MyQHolder {
                val binding = ReadQuestionItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
                return MyQHolder(binding)
            }
        }
    }

    // 본문 Hodler
    class MyContentHolder(val binding2:ReadContentItemBinding) : RecyclerView.ViewHolder(binding2.root) {

        fun setContentList(item: ReadContentData) {
            //본문내용(텍스트)
            binding2.docContent.text = item.docContent

            //사진 띄우기 **** - 나중에 하기.
            if(item.contentImg != null)
            {
                //binding.contentImg.setImageBitmap()
            }else{
                binding2.contentImg.visibility = View.VISIBLE
            }



            if(item.linkUri != ""){
                binding2.clLinkArea.visibility = item.linkLayout?.visibility!!
                loadLink(item.linkUri.toString())
            }else{
                binding2.clLinkArea.visibility = View.GONE
            }
        }


        fun setLink(linkUri: String, title: String, content:String, bm1:Bitmap?)
        {
            binding2.linkUri.text = linkUri
            binding2.linkTitle.text = title
            binding2.linkContent.text = content
            binding2.linkIcon.setImageBitmap(bm1)
        }

        private fun loadLink(linkUri: String){
            // 링크 삽입 관련 메소드
            var title: String = ""
            var bm1: Bitmap? = null
            var url1: URL? = null
            var content:String = ""
            isrun = true
            Thread(Runnable {
                while(isrun)
                {//네이버의 경우에만 해당되는 것 같아.
                    try{
                        if (linkUri.contains("naver")) {
                            //linkIcon에 파비콘 추출해서 삽입하기
                            val doc = Jsoup.connect("${linkUri}").get()

                            //제목 들고 오기
                            val link2 = doc.select("body").select("iframe[id=mainFrame]").attr("src")//.attr("content")
                            if(linkUri.contains("blog"))
                            {
                                val doc2 = Jsoup.connect("https://blog.naver.com/${link2}").get()
                                title = doc2.title()
                                content = doc2.select("meta[property=\"og:description\"]").attr("content")
                            }else if(linkUri == "https://www.naver.com/"){
                                title = doc.title()
                                content = doc.select("meta[name=\"og:description\"]").attr("content")
                            }else{
                                title = doc.title()
                                content = doc.select("meta[property=\"og:description\"]").attr("content")
                            }
                            url1 = URL("https://ssl.pstatic.net/sstatic/search/favicon/favicon_191118_pc.ico")
                            var conn: URLConnection = url1!!.openConnection()
                            conn.connect()
                            var bis: BufferedInputStream = BufferedInputStream(conn.getInputStream())
                            bm1 = BitmapFactory.decodeStream(bis)
                            bis.close()
                            setLink(linkUri, title, content, bm1)

                            isrun=false
                        } else {
                            val doc = Jsoup.connect("${linkUri}").get()
                            var favicon:String
                            var link:String
                            if(linkUri.contains("google"))
                            {
                                favicon = doc.select("meta[itemprop=\"image\"]").attr("content")
                                link = "https://www.google.com"+favicon
                                url1 = URL("${link}")
                            }else{
                                //파비콘 이미지 들고 오기
                                favicon = doc.select("link[rel=\"icon\"]").attr("href")
                                if(favicon=="")
                                {
                                    favicon = doc.select("link[rel=\"SHORTCUT ICON\"]").attr("href")
                                }
                                if (!favicon.contains("https:")) {
                                    link = "https://"+favicon
                                    url1 = URL("${link}")
                                }else{
                                    url1 = URL("${favicon}")
                                }
                            }

                            try{
                                var conn: URLConnection = url1!!.openConnection()
                                conn.connect()
                                var bis: BufferedInputStream = BufferedInputStream(conn.getInputStream())
                                bm1 = BitmapFactory.decodeStream(bis)
                                bis.close()
                            }catch (e:Exception)
                            {
                            }
                            title = doc.title()

                            content = doc.select("meta[name=\"description\"]").attr("content")
                            if(content == "")
                            {
                                content = doc.select("meta[property=\"og:site_name\"]").attr("content")
                            }
                            if(title == "")
                            {
                                title = doc.select("meta[property=\"og:site_name\"]").attr("content")
                            }
                            if(bm1==null)
                            {
                                binding2.linkIcon.visibility= View.GONE
                            }
                            setLink(linkUri, title, content, bm1)
                            isrun=false
                        }
                    }catch(e:Exception){
                        //링크가 올바르지 않을때->안내 토스트 메시지를 띄움

                    }
                }

            }).start()
        }

        companion object Factory {
            fun create(parent: ViewGroup): MyContentHolder {
                val binding2 = ReadContentItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
                return MyContentHolder(binding2)
            }
        }
    }


    override fun getItemCount() = items.size

    fun addItems(item: ReadItem) {
        this.items.add(item)
        this.notifyDataSetChanged()
    }
}