You should be able to compile and run the java code from the command line.

tar xvf cs4480.tar
cd cs4480
javac *.java
cd ..
java cs4480/ProxyCache 6000

(my java files are in the package cs4480, so if you don't use this filename it won't link correctly)

Proxy cache is the main java file.
I've been using port 6000 but you can use any port you want to (between 6000 and 8000), 
just enter it in as the command line argument.
Telnet in putty is what I have been using, again connecting to localhost and port 6000, and using
the settings for telnet, switching telnet over to passive mode, and changing the window to close never.

Entering in GET http://www.google.com HTTP/1.0 will submit a request for google.com if you
enter in an empty line after the request.

Same if you use relative addressing, such as:
GET / HTTP/1.0
Host: www.google.com

With Firefox, I had trouble with pages having a different encoding that the browser couldn't interpret. I had success with
google.com, yahoo.com, and facebook.com.
