package com.matrix.me

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.matrix.me.engine.MatchEngineActor
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class MatchEngineActorSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll: Unit = {
//    system.actorOf(MatchEngineActor.props)

    system.actorOf(Props(new MatchEngineActor()), "HelloWorldActor")
  }


  "A MatchEngineActor" must {

  }


}
