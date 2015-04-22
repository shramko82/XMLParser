package ua.goit.xmlparsertdd.parsers;

import ua.goit.xmlparsertdd.statemachine.TagStateMachine;
import ua.goit.xmlparsertdd.exceptions.XMLNestingException;
import ua.goit.xmlparsertdd.elements.Element;
import ua.goit.xmlparsertdd.statemachine.Event;
import ua.goit.xmlparsertdd.statemachine.TagState;
import ua.goit.xmlparsertdd.handlers.Handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class XMLParser implements Parser {
  private TagStateMachine machine = new TagStateMachine();
  private final Map<Event, Set<Handler>> handlers;

  private XMLParser(Map<Event, Set<Handler>> handlers) {
    this.handlers = handlers;
  }
  
  @Override
  public void parse(String strArg) {
    parse(new ByteArrayInputStream(strArg.getBytes()));
  }

  @Override
  public void parse(File strArg) throws FileNotFoundException{
    parse(new FileInputStream(strArg));
  }

  @Override
  public void parse(InputStream iStreamReader) {
    try (InputStreamReader inputStreamReader = new InputStreamReader(iStreamReader)) {
      char c;
      TagState currentState = null;
      try {
        while (inputStreamReader.ready()) {
          c = (char) inputStreamReader.read();
            currentState = machine.next(c, this);
          if(currentState == TagState.INVALID_TAG_END) {
            return;
          }
        }
      } catch (XMLNestingException e) {
        //ignoring
      }
      if (currentState == TagState.VALID_TAG_END) {
        if (machine.isStackEmpty()) {
          machine.createSuccessTextElement(this, "Parsing success");
        } else {
          machine.createErrorTextElement(this, "Some tags aren't closed");
        }
      } else {
        machine.createErrorTextElement(this, "Incorrect XML code");
      }
    } catch (IOException e) {
        e.printStackTrace();
    }
  }

  public void sendEventToHandler(Event event, Element result) {
    Set<Handler> set = handlers.get(event);
    if (set == null) {return;}
    for (Handler handler : set) {
      handler.handle(result);
    }
  }

  public static class Builder {
    private Map<Event, Set<Handler>> handlers = new HashMap<>();

    public void onOpenTag(Handler handler) {
      registerHandlerOnEvent(handler, Event.OPEN_TAG);
    }

    public void onTextValue(Handler handler) {
      registerHandlerOnEvent(handler, Event.TEXT_VALUE);
    }

    public void onStart(Handler handler) {
      registerHandlerOnEvent(handler, Event.START);
    }

    public void onEnd(Handler handler) {
      registerHandlerOnEvent(handler, Event.VALID_END);
    }

    public void onError(Handler handler) {
      registerHandlerOnEvent(handler, Event.INVALID_END);
    }

    public void onCloseTag(Handler handler) {
      registerHandlerOnEvent(handler, Event.CLOSE_TAG);
    }

    public void onSingleTag(Handler handler) {
      registerHandlerOnEvent(handler, Event.SINGLE_TAG);
    }

    private void registerHandlerOnEvent(Handler handler, Event event) {
      if (handler != null) {
        Set<Handler> set;
        if (handlers.containsKey(event)) {
          set = handlers.get(event);
        } else {
          set = new HashSet<>();
        }
        set.add(handler);
        handlers.put(event, set);
      } else {
        throw new NullPointerException();
      }
    }

    public static Builder newParserBuilder() {
      return new Builder();
    }

    public XMLParser build() {
      return new XMLParser(handlers);
    }
  }
}
