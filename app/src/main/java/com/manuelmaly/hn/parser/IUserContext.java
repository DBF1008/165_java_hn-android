package com.manuelmaly.hn.parser;

/**
 * Supplies the currently logged-in HN username to the parsers, replacing their
 * direct {@code Settings.getUserName(App.getInstance())} coupling. Implemented by
 * the settings repository.
 */
public interface IUserContext {

    String getCurrentUsername();

}
