# https://stedolan.github.io/jq/tutorial/

curl http://ana:8080/tell/json -d\
"`curl \
    'http://samples.openweathermap.org/data/2.5/weather?lat=35&lon=139&appid=b1b15e88fa797225412429c1c50c122a1' \
    | \
    jq '.I=("OpenWeather/"+(.id|tostring)) | .N=("Weather: "+(.name|tostring)+ " " +(.dt|todate)) | del(.id) | del(.name)' \
`"

