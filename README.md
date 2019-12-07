# key-metrics

Key Metrics is a Clojure library designed to provide analysis of keylog info provided by your favorite keylogger. It handles pomodoro scheduling, break reminders, provides historical data for visualization of work trends, along with some rudimentary text based charting, and in general helps quantify your life. It is still under development. 

## Usage
The keylogger I am using can be found here: 
[Website](https://simple-keylogger.github.io) - [Keylogger wiki](https://github.com/GiacomoLaw/Keylogger/wiki)

```c
    time_t rawtime;
    struct tm * timeinfo;

    time ( &rawtime );
    timeinfo = localtime ( &rawtime );
    /* printf ( "Current local time and date: %s", asctime (timeinfo) ); */

    fprintf(logfile, "%s :: ", convertKeyCode(keyCode));
    fprintf(logfile, "%s", asctime(timeinfo));
    ```
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
