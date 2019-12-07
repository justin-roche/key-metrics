# key-metrics

Key Metrics is a Clojure library designed to provide analysis of keylog info provided by your favorite keylogger. It handles pomodoro scheduling, break reminders, provides historical data for visualization of work trends, along with some rudimentary text based charting, and in general helps quantify your life. It is still under development. 

## Usage
The keylogger I am using can be found here: [Simple Keylogger](https://simple-keylogger.github.io) 

For Key Metrics to make use of the keylogger's logfile, the source code for the keylogger needs to be updated to add a timestamp for key records. For Simple Keylogger, include time.h in the headers and import it, then in keylogger.c, add the following: 

```c
    time_t rawtime;
    struct tm * timeinfo;

    time ( &rawtime );
    timeinfo = localtime ( &rawtime );

    fprintf(logfile, "%s :: ", convertKeyCode(keyCode));
    fprintf(logfile, "%s", asctime(timeinfo));
```

Alternatively, build the forked version which already does this: [Simple Keylogger With Timestamps](https://github.com/justin-roche/simple-keylogger).

## License

Copyright © 2019 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
