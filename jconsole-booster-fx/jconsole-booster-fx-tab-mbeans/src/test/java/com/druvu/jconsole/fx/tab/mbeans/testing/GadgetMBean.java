package com.druvu.jconsole.fx.tab.mbeans.testing;

/**
 * The management interface of {@link Gadget}.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public interface GadgetMBean {

    int getLevel();

    void setLevel(int level);

    String getName();

    /** Always fails, to show an unavailable attribute. */
    String getBroken();

    long[] getHistory();

    String echo(String text);

    void rename(String newName);

    void reset();

    /** Sends a notification with the message. */
    void ping(String message);
}
