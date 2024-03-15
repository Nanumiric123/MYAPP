

class commonFunctions  {
    var finalResult:MutableList<dataRequestor> = mutableListOf()
}

data class listData(
    var ID:Int,
    var MATERIAL:String,
    var LOCATION:String,
    var QUANTITY:Int,
    var REQUESTOR: String,
    var PULLLISTNUMBER:String
)

data class dataRequestor(
    var listData:List<listData>,
    var PULLLIST: String
)