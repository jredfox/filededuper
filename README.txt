Commands:
use help to list the comamnds
command format is: commandId --optionalArgs params
optional arguments could be parsed anywhere it will work properly however, I it's good practice to use optional args either first or last as other programs may error

Command Options:
option command char starts with one dash and can be combined with multiple others in one statement example: -c or combined -abcd where it's the same as -a -b -c -d
option command for a string starts with two dashes. example: --stacktrace
option command value can be assigned to a single char option or a stack trace: -c=value or --mainClass="com.jredfox.filededuper.Main.class" or --aString="\"a quoted string\""

Command API:
Ids are lower cased enforced
use ParamsList#getFlag if you use your command option as flag
use ParamsList#getValue if your command option has a value
escaped quotes is \"
escaped escaped sequence is \\

Plugin API:
NOT DONE YET

License:
File De-Duper is a programming utiltity to create, compile and compare archives. Originally created for both me and omni-archive uses
This program is currently free of charge but, that may change in the future
You may re-distrobute free versions of this program
You may modify this program on the premise that you don't modify the newly created classes so I know the jar is modded
You may not use this program to create fake timestamps and fake jar / archive files. The command setTimeStampArchive is used to repair eclipse compiled jars and to make consistent jars after compiled

