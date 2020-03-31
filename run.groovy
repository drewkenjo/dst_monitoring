#!/home/kenjo/.groovy/coatjava/bin/run-groovy
import org.jlab.io.hipo.HipoDataSource
import org.jlab.detector.base.DetectorType
import org.jlab.clas.physics.Vector3
import org.jlab.groot.data.H1F
import org.jlab.groot.data.H2F
import org.jlab.groot.data.TDirectory
import groovyx.gpars.GParsPool
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import mon.clas12.exclusive.ep_mon
import mon.clas12.exclusive.eppi0_mon
import mon.clas12.exclusive.eppippim_mon
import mon.clas12.exclusive.enpip_mon
import mon.clas12.exclusive.epip_mon
import mon.clas12.exclusive.epippim_mon

MyMods.enable()
/////////////////////////////////////////////////////////////////////

def outname = args[0].split('/')[-1]

def processors = [new eppi0_mon(), new ep_mon(), new eppippim_mon()]
#processors = [new enpip_mon(), new epip_mon()]

def evcount = new AtomicInteger()
def save = {
  processors.each{
    def out = new TDirectory()
    out.mkdir("/root")
    out.cd("/root")
    it.hists.each{out.writeDataSet(it.value)}
    def clasname = it.getClass().getSimpleName()
    out.writeFile("mon_${clasname}_${outname}")
  }
  println "event count: "+evcount.get()
  evcount.set(0)
}

def exe = Executors.newScheduledThreadPool(1)
exe.scheduleWithFixedDelay(save, 5, 30, TimeUnit.SECONDS)

GParsPool.withPool 12, {
  args.eachParallel{fname->
    def reader = new HipoDataSource()
    reader.open(fname)

    //while(reader.hasEvent() && evcount.get()<5000000) {
    while(reader.hasEvent()) {
      evcount.getAndIncrement()
      def event = reader.getNextEvent()
      processors.each{it.processEvent(event)}
    }

    reader.close()
  }
}

exe.shutdown()
save()
