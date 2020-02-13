package in.handyman.process.onethread

import com.typesafe.scalalogging.LazyLogging
import in.handyman.command.Context
import in.handyman.command.Action
import in.handyman.command.CommandProxy
import in.handyman.util.ResourceAccess
import org.json.JSONObject
import org.json.JSONArray
import org.eclipse.emf.common.util.EList
import in.handyman.util.ParameterisationEngine
import java.util.ArrayList
import java.sql.SQLException
import scala.util.control.Exception.Finally
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.impl.client.DefaultHttpClient
import com.google.gson.Gson
import org.apache.http.entity.StringEntity

class ModelTrainingAction extends in.handyman.command.Action with LazyLogging {
  val detailMap = new java.util.HashMap[String, String]

  def execute(context: in.handyman.command.Context, action: in.handyman.dsl.Action): in.handyman.command.Context = {
    val modeltrainingAsIs: in.handyman.dsl.Mahout = action.asInstanceOf[in.handyman.dsl.Mahout]
    val modeltraining: in.handyman.dsl.Mahout = CommandProxy.createProxy(modeltrainingAsIs, classOf[in.handyman.dsl.Mahout], context)

    try {
      val postData = modeltraining.getValue
      val gson = new Gson
      // create an HttpPost object
      // val url = modeltraining.getUrl
      val post = new HttpPost("http://localhost:8008")
      //  val hkey = modeltraining.getHkey
      //  val hvalue = modeltraining.getHvalue

      // set the Content-type
      post.setHeader("Content-type", "application/json")

      // add the JSON as a StringEntity
      post.setEntity(new StringEntity(gson.toJson(postData)))
      // send the post request
      val response = (new DefaultHttpClient).execute(post)
      var content = ""
      val entity = response.getEntity()
      if (entity != null) {
        val inputStream = entity.getContent()
        content = scala.io.Source.fromInputStream(inputStream).getLines.mkString
        inputStream.close
      }

      val resultObj = new JSONObject(content.toString())
      if (resultObj.getString("message").equals("success")) {
        println("success")
        true

      } else {
        println("error")
      }
      logger.info("response from python script  #{}", resultObj.getString("message"))
      false
    } catch {
      case e: Exception => false
    }
    context
  }

  def executeIf(context: in.handyman.command.Context, action: in.handyman.dsl.Action): Boolean = {
    val modeltrainingAsIs: in.handyman.dsl.Mahout = action.asInstanceOf[in.handyman.dsl.Mahout]
    val modeltraining: in.handyman.dsl.Mahout = CommandProxy.createProxy(modeltrainingAsIs, classOf[in.handyman.dsl.Mahout], context)

    val expression = modeltraining.getCondition
    try {
      val output = ParameterisationEngine.doYieldtoTrue(expression)
      detailMap.putIfAbsent("condition-output", output.toString())
      output
    } finally {
      if (expression != null)
        detailMap.putIfAbsent("condition", "LHS=" + expression.getLhs + ", Operator=" + expression.getOperator + ", RHS=" + expression.getRhs)

    }
  }
  def generateAudit(): java.util.Map[String, String] = {
    detailMap
  }
}
