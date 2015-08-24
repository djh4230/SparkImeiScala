package com.llyt.sparkstream

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import com.google.gson.JsonParser
import org.apache.spark.streaming.kafka.KafkaUtils
import scala.collection.mutable.HashMap
import com.llyt.util.HbaseUtil
import scala.collection.JavaConversions._



object ImeiSparkstream {
	
  def main(args:Array[String]){
    val zkQuorum="192.168.161.9:2181,192.168.161.12:2181,192.168.161.11:2181,";
    val groupId="cusumer-001";
    val topic="imei1";
    
    val conf=new SparkConf().setAppName("ImeiStream").setMaster("spark://RM-WEB02:7077")
    val ssc=new StreamingContext(conf,Seconds(2))
    var map=Map(topic->2)
    val kafkaStream=KafkaUtils.createStream(ssc, zkQuorum, groupId,map)
    val lines=kafkaStream.map(_._2);
    lines.foreach(l => {
      val arrays=l.collect;
      for(str <- arrays){
        var hbaseUtil = HbaseUtil.getInstance("imei", "imei");
        var tuple2=parseJson(str);
        var body=tuple2._2;
        var items:Array[String]=body.split("\t");
        var imeiID=items(0);
        var imeiData=items(2);
        var contents=imeiData.split(" ");
        var result=new HashMap[String,Integer];
        for(content<-contents){
          var keyword=content.split("\\|")
          var keys=keyword(0).split("/")
          var value:Int=Integer.valueOf(keyword(1))
          var j=0
          for(key<-keys){
            if(result.contains(key)){
              j=result.apply(key)
              result.update(key, value)
            }
          }
        }
        hbaseUtil.put1(imeiID, result);
        
      }
      Unit
    })
    ssc.start();
    ssc.awaitTermination();
  }
  def parseJson(message:String):Tuple2[String,String]={
    val jsonParser:JsonParser=new JsonParser()
    val parsonObject=jsonParser.parse(message).getAsJsonObject()
    val header:String=parsonObject.get("header").getAsString()
    val body:String=parsonObject.get("body").getAsString()
    val tuple2:Tuple2[String,String]=new Tuple2(header,body)
    tuple2
  }
}