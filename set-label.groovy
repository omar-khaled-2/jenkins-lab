import jenkins.model.*
import jenkins.*

def instance = Jenkins.getInstance()
def node = instance.getNode("")

if (node != null) {
    node.setLabelString("main")
    node.save()
    instance.save()
    println "Set label 'main' on built-in node"
} else {
    println "Could not find built-in node"
}
