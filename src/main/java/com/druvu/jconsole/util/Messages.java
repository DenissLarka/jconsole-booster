/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.druvu.jconsole.util;

/** Class that contains localized messages (English only, hardcoded). */
public final class Messages {
    private Messages() {}

    public static final String ONE_DAY = " 1 day";
    public static final String ONE_HOUR = " 1 hour";
    public static final String ONE_MIN = " 1 min";
    public static final String ONE_MONTH = " 1 month";
    public static final String ONE_YEAR = " 1 year";
    public static final String TWO_HOURS = " 2 hours";
    public static final String THREE_HOURS = " 3 hours";
    public static final String THREE_MONTHS = " 3 months";
    public static final String FIVE_MIN = " 5 min";
    public static final String SIX_HOURS = " 6 hours";
    public static final String SIX_MONTHS = " 6 months";
    public static final String SEVEN_DAYS = " 7 days";
    public static final String TEN_MIN = "10 min";
    public static final String TWELVE_HOURS = "12 hours";
    public static final String THIRTY_MIN = "30 min";
    public static final String LESS_THAN = "<";
    public static final String A_LOT_LESS_THAN = "<<";
    public static final String GREATER_THAN = ">";
    public static final String ACTION_CAPITALIZED = "ACTION";
    public static final String ACTION_INFO_CAPITALIZED = "ACTION_INFO";
    public static final String ALL = "All";
    public static final String ARCHITECTURE = "Architecture";
    public static final String ATTRIBUTE = "Attribute";
    public static final String ATTRIBUTE_VALUE = "Attribute value";
    public static final String ATTRIBUTE_VALUES = "Attribute values";
    public static final String ATTRIBUTES = "Attributes";
    public static final String BLANK = "Blank";
    public static final String BLOCKED_COUNT_WAITED_COUNT = "Total blocked: {0}  Total waited: {1}\n";
    public static final String BOOT_CLASS_PATH = "Boot class path";
    public static final String BORDERED_COMPONENT_MORE_OR_LESS_BUTTON_TOOLTIP =
            "Toggle to show more or less information";
    public static final String CPU_USAGE = "CPU Usage";
    public static final String CPU_USAGE_FORMAT = "CPU Usage: {0}%";
    public static final String CANCEL = "Cancel";
    public static final String CASCADE = "Cascade";
    public static final String CHART_COLON = "Chart:";
    public static final String CLASS_PATH = "Class path";
    public static final String CLASS_NAME = "ClassName";
    public static final String CLASS_TAB_INFO_LABEL_FORMAT = "<html>Loaded: {0}    Unloaded: {1}    Total: {2}</html>";
    public static final String CLASS_TAB_LOADED_CLASSES_PLOTTER_ACCESSIBLE_NAME = "Chart for Loaded Classes.";
    public static final String CLASSES = "Classes";
    public static final String CLOSE = "Close";
    public static final String COLUMN_NAME = "Name";
    public static final String COLUMN_PID = "PID";
    public static final String COMMITTED_MEMORY = "Committed memory";
    public static final String COMMITTED_VIRTUAL_MEMORY = "Committed virtual memory";
    public static final String COMMITTED = "Committed";
    public static final String CONNECT = "Connect";
    public static final String CONNECT_DIALOG_CONNECT_BUTTON_TOOLTIP = "Connect to Java Virtual Machine";
    public static final String CONNECT_DIALOG_ACCESSIBLE_DESCRIPTION =
            "Dialog for making a new connection to a local or remote Java Virtual Machine";
    public static final String CONNECT_DIALOG_MASTHEAD_ACCESSIBLE_NAME = "Masthead Graphic";
    public static final String CONNECT_DIALOG_MASTHEAD_TITLE = "New Connection";
    public static final String CONNECT_DIALOG_STATUS_BAR_ACCESSIBLE_NAME = "Status Bar";
    public static final String CONNECT_DIALOG_TITLE = "JConsole: New Connection";
    public static final String CONNECTED_PUNCTUATION_CLICK_TO_DISCONNECT_ = "Connected. Click to disconnect.";
    public static final String CONNECTION_FAILED = "Connection failed";
    public static final String CONNECTION = "Connection";
    public static final String CONNECTION_NAME = "Connection name";
    public static final String CONNECTION_NAME__DISCONNECTED_ = "{0} (disconnected)";
    public static final String CONSTRUCTOR = "Constructor";
    public static final String CURRENT_CLASSES_LOADED = "Current classes loaded";
    public static final String CURRENT_HEAP_SIZE = "Current heap size";
    public static final String CURRENT_VALUE = "Current value: {0}";
    public static final String CREATE = "Create";
    public static final String DAEMON_THREADS = "Daemon threads";
    public static final String DISCONNECTED_PUNCTUATION_CLICK_TO_CONNECT_ = "Disconnected. Click to connect.";
    public static final String DOUBLE_CLICK_TO_EXPAND_FORWARD_SLASH_COLLAPSE = "Double click to expand/collapse";
    public static final String DOUBLE_CLICK_TO_VISUALIZE = "Double click to visualize";
    public static final String DESCRIPTION = "Description";
    public static final String DESCRIPTOR = "Descriptor";
    public static final String DETAILS = "Details";
    public static final String DETECT_DEADLOCK = "Detect Deadlock";
    public static final String DETECT_DEADLOCK_TOOLTIP = "Detect deadlocked threads";
    public static final String DIMENSION_IS_NOT_SUPPORTED_COLON = "Dimension is not supported:";
    public static final String DISCARD_CHART = "Discard chart";
    public static final String DURATION_DAYS_HOURS_MINUTES =
            "{0,choice,1#{0,number,integer} day |1.0<{0,number,integer} days }{1,choice,0<{1,number,integer} hours |1#{1,number,integer} hour |1<{1,number,integer} hours }{2,choice,0<{2,number,integer} minutes|1#{2,number,integer} minute|1.0<{2,number,integer} minutes}";
    public static final String DURATION_HOURS_MINUTES =
            "{0,choice,1#{0,number,integer} hour |1<{0,number,integer} hours }{1,choice,0<{1,number,integer} minutes|1#{1,number,integer} minute|1.0<{1,number,integer} minutes}";
    public static final String DURATION_MINUTES =
            "{0,choice,1#{0,number,integer} minute|1.0<{0,number,integer} minutes}";
    public static final String DURATION_SECONDS = "{0} seconds";
    public static final String EMPTY_ARRAY = "Empty array";
    public static final String ERROR = "Error";
    public static final String ERROR_COLON_MBEANS_ALREADY_EXIST = "Error: MBeans already exist";
    public static final String ERROR_COLON_MBEANS_DO_NOT_EXIST = "Error: MBeans do not exist";
    public static final String EVENT = "Event";
    public static final String EXIT = "Exit";
    public static final String FILE_CHOOSER_FILE_EXISTS_CANCEL_OPTION = "Cancel";
    public static final String FILE_CHOOSER_FILE_EXISTS_MESSAGE =
            "<html><center>File already exists:<br>{0}<br>Do you want to replace it?";
    public static final String FILE_CHOOSER_FILE_EXISTS_OK_OPTION = "Replace";
    public static final String FILE_CHOOSER_FILE_EXISTS_TITLE = "File Exists";
    public static final String FILE_CHOOSER_SAVED_FILE = "<html>Saved to file:<br>{0}<br>({1} bytes)";
    public static final String FILE_CHOOSER_SAVE_FAILED_MESSAGE = "<html><center>Save to file failed:<br>{0}<br>{1}";
    public static final String FILE_CHOOSER_SAVE_FAILED_TITLE = "Save Failed";
    public static final String FREE_PHYSICAL_MEMORY = "Free physical memory";
    public static final String FREE_SWAP_SPACE = "Free swap space";
    public static final String GARBAGE_COLLECTOR = "Garbage collector";
    public static final String GC_INFO =
            "Name = ''{0}'', Collections = {1,choice,-1#Unavailable|0#{1,number,integer}}, Total time spent = {2}";
    public static final String GC_TIME = "GC time";
    public static final String GC_TIME_DETAILS = "{0} on {1} ({2} collections)";
    public static final String HEAP_MEMORY_USAGE = "Heap Memory Usage";
    public static final String HEAP = "Heap";
    public static final String HELP_ABOUT_DIALOG_ACCESSIBLE_DESCRIPTION =
            "Dialog containing information about JConsole and JDK versions";
    public static final String HELP_ABOUT_DIALOG_JCONSOLE_VERSION = "JConsole version:<br>{0}";
    public static final String HELP_ABOUT_DIALOG_JAVA_VERSION = "Java VM version:<br>{0}";
    public static final String HELP_ABOUT_DIALOG_MASTHEAD_ACCESSIBLE_NAME = "Masthead Graphic";
    public static final String HELP_ABOUT_DIALOG_MASTHEAD_TITLE = "About JConsole";
    public static final String HELP_ABOUT_DIALOG_TITLE = "JConsole: About";
    public static final String HELP_ABOUT_DIALOG_USER_GUIDE_LINK_URL =
            "http://www.oracle.com/pls/topic/lookup?ctx=javase{0}&id=using_jconsole";
    public static final String HELP_MENU_ABOUT_TITLE = "About JConsole";
    public static final String HELP_MENU_USER_GUIDE_TITLE = "Online User Guide";
    public static final String HELP_MENU_TITLE = "Help";
    public static final String HOTSPOT_MBEANS_ELLIPSIS = "Hotspot MBeans...";
    public static final String HOTSPOT_MBEANS_DIALOG_ACCESSIBLE_DESCRIPTION = "Dialog for managing Hotspot MBeans";
    public static final String IMPACT = "Impact";
    public static final String INFO = "Info";
    public static final String INFO_CAPITALIZED = "INFO";
    public static final String INSECURE = "Insecure connection";
    public static final String INVALID_URL = "Invalid URL: {0}";
    public static final String IS = "Is";
    public static final String JAVA_MONITORING___MANAGEMENT_CONSOLE = "Java Monitoring & Management Console";
    public static final String JCONSOLE_COLON_ = "JConsole: {0}";
    public static final String JCONSOLE_VERSION = "JConsole version \"{0}\"";
    public static final String JCONSOLE_ACCESSIBLE_DESCRIPTION = "Java Monitoring & Management Console";
    public static final String JIT_COMPILER = "JIT compiler";
    public static final String LIBRARY_PATH = "Library path";
    public static final String LIVE_THREADS = "Live threads";
    public static final String LOADED = "Loaded";
    public static final String LOCAL_PROCESS_COLON = "Local Process:";
    public static final String MASTHEAD_FONT = "Dialog-PLAIN-25";
    public static final String MANAGEMENT_NOT_ENABLED =
            "<b>Note</b>: The management agent is not enabled on this process.";
    public static final String MANAGEMENT_WILL_BE_ENABLED =
            "<b>Note</b>: The management agent will be enabled on this process.";
    public static final String MBEAN_ATTRIBUTE_INFO = "MBeanAttributeInfo";
    public static final String MBEAN_INFO = "MBeanInfo";
    public static final String MBEAN_NOTIFICATION_INFO = "MBeanNotificationInfo";
    public static final String MBEAN_OPERATION_INFO = "MBeanOperationInfo";
    public static final String MBEANS = "MBeans";
    public static final String MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON = "Clear";
    public static final String MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON_TOOLTIP = "Clear notifications";
    public static final String MBEANS_TAB_COMPOSITE_NAVIGATION_MULTIPLE = "Composite Data Navigation {0}/{1}";
    public static final String MBEANS_TAB_COMPOSITE_NAVIGATION_SINGLE = "Composite Data Navigation";
    public static final String MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON = "Refresh";
    public static final String MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON_TOOLTIP = "Refresh attributes";
    public static final String MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON = "Subscribe";
    public static final String MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON_TOOLTIP = "Start listening for notifications";
    public static final String MBEANS_TAB_TABULAR_NAVIGATION_MULTIPLE = "Tabular Data Navigation {0}/{1}";
    public static final String MBEANS_TAB_TABULAR_NAVIGATION_SINGLE = "Tabular Data Navigation";
    public static final String MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON = "Unsubscribe";
    public static final String MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON_TOOLTIP = "Stop listening for notifications";
    public static final String MANAGE_HOTSPOT_MBEANS_IN_COLON_ = "Manage Hotspot MBeans in:";
    public static final String MAX = "Max";
    public static final String MAXIMUM_HEAP_SIZE = "Maximum heap size";
    public static final String MEMORY = "Memory";
    public static final String MEMORY_POOL_LABEL = "Memory Pool \"{0}\"";
    public static final String MEMORY_TAB_HEAP_PLOTTER_ACCESSIBLE_NAME = "Memory usage chart for heap.";
    public static final String MEMORY_TAB_INFO_LABEL_FORMAT = "<html>Used: {0}    Committed: {1}    Max: {2}</html>";
    public static final String MEMORY_TAB_NON_HEAP_PLOTTER_ACCESSIBLE_NAME = "Memory usage chart for non heap.";
    public static final String MEMORY_TAB_POOL_CHART_ABOVE_THRESHOLD = "which is above the threshold of {0}.\n";
    public static final String MEMORY_TAB_POOL_CHART_ACCESSIBLE_NAME = "Memory Pool Usage Chart.";
    public static final String MEMORY_TAB_POOL_CHART_BELOW_THRESHOLD = "which is below the threshold of {0}.\n";
    public static final String MEMORY_TAB_POOL_PLOTTER_ACCESSIBLE_NAME = "Memory usage chart for {0}.";
    public static final String MESSAGE = "Message";
    public static final String METHOD_SUCCESSFULLY_INVOKED = "Method successfully invoked";
    public static final String MINIMIZE_ALL = "Minimize All";
    public static final String MONITOR_LOCKED = "   - locked {0}\n";
    public static final String NAME = "Name";
    public static final String NAME_AND_BUILD = "{0} (build {1})";
    public static final String NAME_STATE = "Name: {0}\nState: {1}\n";
    public static final String NAME_STATE_LOCK_NAME = "Name: {0}\nState: {1} on {2}\n";
    public static final String NAME_STATE_LOCK_NAME_LOCK_OWNER = "Name: {0}\nState: {1} on {2} owned by: {3}\n";
    public static final String NEW_CONNECTION_ELLIPSIS = "New Connection...";
    public static final String NO_DEADLOCK_DETECTED = "No deadlock detected";
    public static final String NON_HEAP_MEMORY_USAGE = "Non-Heap Memory Usage";
    public static final String NON_HEAP = "Non-Heap";
    public static final String NOTIFICATION = "Notification";
    public static final String NOTIFICATION_BUFFER = "Notification buffer";
    public static final String NOTIFICATIONS = "Notifications";
    public static final String NOTIF_TYPES = "NotifTypes";
    public static final String NUMBER_OF_THREADS = "Number of Threads";
    public static final String NUMBER_OF_LOADED_CLASSES = "Number of Loaded Classes";
    public static final String NUMBER_OF_PROCESSORS = "Number of processors";
    public static final String OBJECT_NAME = "ObjectName";
    public static final String OPERATING_SYSTEM = "Operating System";
    public static final String OPERATION = "Operation";
    public static final String OPERATION_INVOCATION = "Operation invocation";
    public static final String OPERATION_RETURN_VALUE = "Operation return value";
    public static final String OPERATIONS = "Operations";
    public static final String OVERVIEW = "Overview";
    public static final String OVERVIEW_PANEL_PLOTTER_ACCESSIBLE_NAME = "Chart for {0}.";
    public static final String PARAMETER = "Parameter";
    public static final String PASSWORD_COLON_ = "Password:";
    public static final String PASSWORD_ACCESSIBLE_NAME = "Password";
    public static final String PEAK = "Peak";
    public static final String PERFORM_GC = "Perform GC";
    public static final String PERFORM_GC_TOOLTIP = "Request Garbage Collection";
    public static final String PLOTTER_ACCESSIBLE_NAME = "Chart";
    public static final String PLOTTER_ACCESSIBLE_NAME_KEY_AND_VALUE = "{0}={1}\n";
    public static final String PLOTTER_ACCESSIBLE_NAME_NO_DATA = "No data plotted.";
    public static final String PLOTTER_SAVE_AS_MENU_ITEM = "Save data as...";
    public static final String PLOTTER_TIME_RANGE_MENU = "Time Range";
    public static final String PLUGIN_EXCEPTION_DIALOG_BUTTON_EXIT = "Exit";
    public static final String PLUGIN_EXCEPTION_DIALOG_BUTTON_IGNORE = "Ignore";
    public static final String PLUGIN_EXCEPTION_DIALOG_BUTTON_OK = "OK";
    public static final String PLUGIN_EXCEPTION_DIALOG_MESSAGE =
            "An unexpected exception has occurred in %s:\n\n%s\n\nStart with -debug for details. Ignore will suppress further exceptions.";
    public static final String PLUGIN_EXCEPTION_DIALOG_TITLE = "Plug-in exception";
    public static final String PROBLEM_ADDING_LISTENER = "Problem adding listener";
    public static final String PROBLEM_DISPLAYING_MBEAN = "Problem displaying MBean";
    public static final String PROBLEM_INVOKING = "Problem invoking";
    public static final String PROBLEM_REMOVING_LISTENER = "Problem removing listener";
    public static final String PROBLEM_SETTING_ATTRIBUTE = "Problem setting attribute";
    public static final String PROCESS_CPU_TIME = "Process CPU time";
    public static final String READABLE = "Readable";
    public static final String RECONNECT = "Reconnect";
    public static final String REMOTE_PROCESS_COLON = "Remote Process:";
    public static final String REMOTE_PROCESS_TEXT_FIELD_ACCESSIBLE_NAME = "Remote Process";
    public static final String RESTORE_ALL = "Restore All";
    public static final String RETURN_TYPE = "ReturnType";
    public static final String SEQ_NUM = "SeqNum";
    public static final String SIZE_BYTES = "{0,number,integer} bytes";
    public static final String SIZE_GB = "{0} Gb";
    public static final String SIZE_KB = "{0} Kb";
    public static final String SIZE_MB = "{0} Mb";
    public static final String SOURCE = "Source";
    public static final String STACK_TRACE = "\nStack trace: \n";
    public static final String SUMMARY_TAB_HEADER_DATE_TIME_FORMAT = "FULL,FULL";
    public static final String SUMMARY_TAB_PENDING_FINALIZATION_LABEL = "Pending finalization";
    public static final String SUMMARY_TAB_PENDING_FINALIZATION_VALUE = "{0} objects";
    public static final String SUMMARY_TAB_TAB_NAME = "VM Summary";
    public static final String SUMMARY_TAB_VM_VERSION = "{0} version {1}";
    public static final String THREADS = "Threads";
    public static final String THREAD_TAB_INFO_LABEL_FORMAT = "<html>Live: {0}    Peak: {1}    Total: {2}</html>";
    public static final String THREAD_TAB_THREAD_INFO_ACCESSIBLE_NAME = "Thread Information";
    public static final String THREAD_TAB_THREAD_PLOTTER_ACCESSIBLE_NAME = "Chart for number of threads.";
    public static final String THREAD_TAB_INITIAL_STACK_TRACE_MESSAGE = "[No thread selected]";
    public static final String THRESHOLD = "Threshold";
    public static final String TILE = "Tile";
    public static final String TIME_RANGE_COLON = "Time Range:";
    public static final String TIME = "Time";
    public static final String TIME_STAMP = "TimeStamp";
    public static final String TOTAL_LOADED = "Total Loaded";
    public static final String TOTAL_CLASSES_LOADED = "Total classes loaded";
    public static final String TOTAL_CLASSES_UNLOADED = "Total classes unloaded";
    public static final String TOTAL_COMPILE_TIME = "Total compile time";
    public static final String TOTAL_PHYSICAL_MEMORY = "Total physical memory";
    public static final String TOTAL_THREADS_STARTED = "Total threads started";
    public static final String TOTAL_SWAP_SPACE = "Total swap space";
    public static final String TYPE = "Type";
    public static final String UNAVAILABLE = "Unavailable";
    public static final String UNKNOWN_CAPITALIZED = "UNKNOWN";
    public static final String UNKNOWN_HOST = "Unknown Host: {0}";
    public static final String UNREGISTER = "Unregister";
    public static final String UPTIME = "Uptime";
    public static final String USAGE_THRESHOLD = "Usage Threshold";
    public static final String REMOTE_TF_USAGE =
            "<b>Usage</b>: &lt;hostname&gt;:&lt;port&gt; OR service:jmx:&lt;protocol&gt;:&lt;sap&gt;";
    public static final String USED = "Used";
    public static final String USERNAME_COLON_ = "Username:";
    public static final String USERNAME_ACCESSIBLE_NAME = "User Name";
    public static final String USER_DATA = "UserData";
    public static final String VIRTUAL_MACHINE = "Virtual Machine";
    public static final String VM_ARGUMENTS = "VM arguments";
    public static final String VMINTERNAL_FRAME_ACCESSIBLE_DESCRIPTION =
            "Internal frame for monitoring a Java Virtual Machine";
    public static final String VALUE = "Value";
    public static final String VENDOR = "Vendor";
    public static final String VERBOSE_OUTPUT = "Verbose Output";
    public static final String VERBOSE_OUTPUT_TOOLTIP = "Enable verbose output for class loading system";
    public static final String VIEW = "View";
    public static final String WINDOW = "Window";
    public static final String WINDOWS = "Windows";
    public static final String WRITABLE = "Writable";
    public static final String CONNECTION_FAILED1 = "Connection Failed: Retry?";
    public static final String CONNECTION_FAILED2 =
            "The connection to {0} did not succeed.<br>Would you like to try again?";
    public static final String CONNECTION_FAILED_SSL1 = "Secure connection failed. Retry insecurely?";
    public static final String CONNECTION_FAILED_SSL2 =
            "The connection to {0} could not be made using SSL.<br>Would you like to try without SSL?<br>(Username and password will be sent in plain text.)";
    public static final String CONNECTION_LOST1 = "Connection Lost: Reconnect?";
    public static final String CONNECTING_TO1 = "Connecting to {0}";
    public static final String CONNECTING_TO2 =
            "You are currently being connected to {0}.<br>This will take a few moments.";
    public static final String DEADLOCK_TAB = "Deadlock";
    public static final String DEADLOCK_TAB_N = "Deadlock {0}";
    public static final String EXPAND = "expand";
    public static final String KBYTES = "{0} kbytes";
    public static final String PLOT = "plot";
    public static final String VISUALIZE = "visualize";
    public static final String ZZ_USAGE_TEXT =
            "Usage: {0} [ -interval=n ] [ -notile ] [ -version ] [ connection ... ]\n\n"
                    + "  -interval   Set the update interval to n seconds (default is 4 seconds)\n"
                    + "  -notile     Do not tile windows initially (for two or more connections)\n"
                    + "  -version    Print program version\n\n"
                    + "  connection = pid || host:port || JMX URL (service:jmx:<protocol>://...)\n"
                    + "  pid         The process id of a target process\n"
                    + "  host        A remote host name or IP address\n"
                    + "  port        The port number for the remote connection\n\n"
                    + "  -J          Specify the input arguments to the Java virtual machine\n"
                    + "              on which jconsole is running";
}
