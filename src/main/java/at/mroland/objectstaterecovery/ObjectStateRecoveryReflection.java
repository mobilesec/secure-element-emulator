/*
 * Copyright 2013 FH OOe Forschungs & Entwicklungs GmbH, Michael Roland.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.mroland.objectstaterecovery;

//import android.app.Activity;
//import android.os.Bundle;
//import android.util.Log;
//import android.util.Xml;
//import at.mroland.test.unmodified.Car;
//import at.mroland.test.unmodified.User;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.lang.reflect.Field;
//import java.lang.reflect.Modifier;
//import java.util.HashMap;
//import java.util.Map;
//import org.xmlpull.v1.XmlPullParser;
//import org.xmlpull.v1.XmlSerializer;

public class ObjectStateRecoveryReflection /*extends Activity*/ {
//  private static final String LOG_TAG = ObjectStateRecoveryReflection.class.getName();
//  
//  private static Map<Integer, FieldState> referenceMap = new HashMap();
//  private static Map<String, FieldState> objectTree = new HashMap();
//  private static Map<String, Integer> objectReferenceTree = new HashMap();
//  private static Map<String, ClassState> classMap = new HashMap();
//  
//  
//  /** Called when the activity is first created. */
//  @Override
//  public void onCreate(Bundle savedInstanceState) {
//    super.onCreate(savedInstanceState);
//    setContentView(R.layout.main);
//
//    
//    final boolean restoreOnly = true;
//    
//    Car c;
//    
//    if (!restoreOnly) {
//      c = Car.getInstance();
//      Log.d(LOG_TAG, "c after initialization = " + c);
//
//      c.addUser(new User(new byte[] { 1, 1, 1 }, (short)21, (short)1));
//      c.addUser(new User(new byte[] { 2, 2, 2 }, (short)40, (short)0));
//      c.addUser(new User(new byte[] { 3, 1, 4 }, (short)10, (short)6));
//      Log.d(LOG_TAG, "c after adding users = " + c);
//
//      c.setOwner(new byte[] { 1, 2, 3, 4 });
//      Log.d(LOG_TAG, "c after changing owner = " + c);
//
//      c.doAccelerate();
//      c.doAccelerate();
//      Log.d(LOG_TAG, "c after accelerating = " + c);
//
//      c.removeUser(new byte[] { 2, 2, 2 });
//      Log.d(LOG_TAG, "c after removing user = " + c);
//
//      // Save state
//      objectTree.put("c", traverseObject(c, c.getClass(), "c"));
//      traverseClass("at.mroland.test.unmodified.Car");
//      traverseClass("at.mroland.test.unmodified.User");
//      traverseClass("at.mroland.test.unmodified.Wheel");
//      traverseClass("at.mroland.test.unmodified.FrontWheel");
//      
//      try {
//        FileOutputStream ostr = new FileOutputStream(this.getExternalFilesDir(null) + "/serialized_state_dump.xml");
//        XmlSerializer xml = Xml.newSerializer();
//
//        xml.setOutput(ostr, "UTF-8");
//
//        serializeStateToXml(xml);
//
//        ostr.flush();
//        ostr.close();
//      } catch (Exception e) {
//        Log.e(LOG_TAG, "Exception: ", e);
//      }
//    }
//    
//    Car.CAR = null;
//    
//    try {
//      Object x = null;
//      Object y = null;
//      
//      try {
//        FileInputStream istr = new FileInputStream(this.getExternalFilesDir(null) + "/serialized_state_dump.xml");
//        XmlPullParser xml = Xml.newPullParser();
//        xml.setInput(istr, "UTF-8");
//        
//        deserializeStateFromXml(xml);
//      } catch (Exception e) {
//        Log.e(LOG_TAG, "Exception: ", e);
//      }
//      
//      x = objectTree.get("c").getInstance();
//      classMap.get("at.mroland.test.unmodified.Car").restoreClass();
//      classMap.get("at.mroland.test.unmodified.User").restoreClass();
//      classMap.get("at.mroland.test.unmodified.Wheel").restoreClass();
//      classMap.get("at.mroland.test.unmodified.FrontWheel").restoreClass();
//      
//      y = Car.class;
//      
//      Log.d(LOG_TAG, "x after restoring = " + x);
//      Log.d(LOG_TAG, "y after restoring = " + y);
//      
//      ((Car)x).doAccelerate();
//      Log.d(LOG_TAG, "x after accelerating = " + x);
//      if (!restoreOnly) {
//        Log.d(LOG_TAG, "c after accelerating x = " + c);
//      }
//      Log.d(LOG_TAG, "Car.CAR = " + Car.CAR);
//    } catch (Exception e) {
//      Log.e(LOG_TAG, "Exception: ", e);
//    }
//  }
//
//
}
