package com.matrix.me

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.matrix.me.engine.MatchEngineActor

import scala.concurrent.duration._

object MatchEngineApp extends App
{

  val actorSystem = ActorSystem("ClusterSystem")
  val cluster = Cluster(actorSystem)

  val clusterSingletonSettings = ClusterSingletonManagerSettings(actorSystem)
  val clusterSingletonManager = ClusterSingletonManager.props(Props[MatchEngineActor], PoisonPill, clusterSingletonSettings)
  actorSystem.actorOf(clusterSingletonManager, "singletonClusterMatchEngineActor")

  val singletonSimpleActor = actorSystem.actorOf(ClusterSingletonProxy.props(
    singletonManagerPath = "/user/singletonClusterMatchEngineActor",
    settings = ClusterSingletonProxySettings(actorSystem)),
    name = "singletonClusterMatchEngineActorProxy")

  import actorSystem.dispatcher
  actorSystem.scheduler.schedule(10 seconds, 5 seconds, singletonSimpleActor, "TEST")
}
