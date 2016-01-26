PeerJDocxFormatter
==================

Simple cli tool to apply basic page formatting to docx files.

```
usage: java -jar PeerJDocxFormatter.jar
 -h         help
 -i <arg>   input docx file
 -l <arg>   line numbering distance
 -m <arg>   margins left,top,right,bottom
 -o <arg>   output docx file
 -r         remove headers and footers
 -v         version
```

Sample Usage:
 `java -jar PeerJDocxFormatter.jar -i Sample.docx -o Formatted.docx -l 0.5 -i "2.54,2.54,2.54,2.54" -r`


To Build
```
ant
```
