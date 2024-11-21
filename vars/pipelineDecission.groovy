#!groovy
def decidePipeline(Map configMap){
  application = configMap.get("application")
    switch(application){
      case 'nodeJSVM':
        echo "application NodeJS and VM"
        break
      case 'javaVM':
        javaVMCI(configMap)
        break
      default:
        error "unrecognized"
        break
      
    }
  echo " I need to take the decission based on the map you sent"
}
