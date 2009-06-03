waveschema.xml:	waveschema.rnc
		echo "<![CDATA[" >waveschema.xml
		cat waveschema.rnc >>waveschema.xml
		echo "]]>" >>waveschema.xml
