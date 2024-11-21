#!groovy
def decidePipeline(Map configMap){
  application = configMap.get("application")
    switch(application){
      case 'nodeJSVM':
        nodeJSVMCI(configMap)  //nodeJSVMCI.call(configMap)  you can also give like this.But call funtion will automatically call if it is call function.
        break
      case 'javaVM':
        javaVMCI(configMap)
        break
      default:
        error "unrecognized"
        break
      
    }

}
