package frc.team6500.trc.util;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;

import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import frc.team6500.trc.auto.TRCAutoEvent;
import frc.team6500.trc.auto.TRCAutoManager;
import frc.team6500.trc.auto.TRCAutoPath;
import frc.team6500.trc.auto.TRCAutoRoutine;
import frc.team6500.trc.util.TRCTypes.*;

public class TRCConfigParser
{
    static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    static DocumentBuilder builder;
    static Document doc;

    public static String readDefaultConfig()
    {
        try {
            File inFile = new File(Filesystem.getDeployDirectory() + "/config.xml");
            FileInputStream inFileStream = new FileInputStream(inFile);
            byte[] array = new byte[(int) inFile.length()];
            inFileStream.read(array);
            inFileStream.close();
            String xmlData = new String(array, "UTF-8");
            return xmlData;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void initialize(boolean useDefaultInputFile, boolean useDefaultAutoFile)
    {
        try
        {
            builder = factory.newDocumentBuilder();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        String defaultConfig = readDefaultConfig();
        if (useDefaultInputFile)
        {
            parseInputConfig(defaultConfig);
        }
        if (useDefaultAutoFile)
        {
            parseAutoConfig(defaultConfig);
        }
    }

    public static void initialize()
    {
        initialize(true, true);
    }

    public static void parseInputConfig(String configString)
    {
        try
        {
            doc = builder.parse(new InputSource(new StringReader(configString)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Element root = doc.getDocumentElement();
        
        NodeList controllerNodeList = root.getElementsByTagName("controller");
        TRCController controllers[] = new TRCController[controllerNodeList.getLength()];
        for (int i = 0; i < controllerNodeList.getLength(); i++)
        {
            Node controllerNode = controllerNodeList.item(i);
            Element controllerElement = (Element) controllerNode;
            ControllerType controllerType = ControllerType.valueOf(controllerElement.getAttribute("type"));
            TRCController controller = new TRCController(Integer.parseInt(controllerElement.getAttribute("port")), controllerType);
            controllers[i] = controller;
            NodeList controllerBindNodeList = controllerElement.getElementsByTagName("bind");
            for (int j = 0; j < controllerBindNodeList.getLength(); j++)
            {
                if (controllerBindNodeList.item(j).getNodeType() != Node.ELEMENT_NODE) { continue; }
                Element controllerBindElement = (Element) controllerBindNodeList.item(j);
                String bindType = controllerBindElement.getAttribute("type");

                if (bindType.equals("button"))
                {
                    String activeFunc = "";
                    String inactiveFunc = "";
                    try
                    {
                        activeFunc = controllerBindElement.getAttribute("active");
                    } catch (Exception e) {}
                    try
                    {
                        inactiveFunc = controllerBindElement.getAttribute("inactive");
                    } catch (Exception e) {}
                    String buttonStringList[] = controllerBindElement.getTextContent().split(",", 0);
                    int buttonIntList[] = new int[buttonStringList.length];
                    for (int k = 0; k < buttonStringList.length; k++)
                    {
                        buttonIntList[k] = -1;

                        try
                        {
                            buttonIntList[k] = Integer.parseInt(buttonStringList[k]);
                        }
                        catch (NumberFormatException e)
                        {
                            try
                            {
                                if (controller.getType() == ControllerType.Xbox360)
                                {
                                    buttonIntList[k] = XboxController.Button.valueOf(buttonStringList[k]).value;
                                }
                                else //(controller.getType() == ControllerType.Extreme3D)
                                {
                                    buttonIntList[k] = Joystick.ButtonType.valueOf(buttonStringList[k]).value;
                                }
                            }
                            catch (Exception e2) { }
                        }
                        if (buttonIntList[k] == -1)
                        {
                            System.out.println("Error interpreting button bind \"" + buttonStringList[k] + "\" (" + controllerElement.getAttribute("type") + " controller on port " + controllerElement.getAttribute("port") + ", name \"" + controllerBindElement.getAttribute("name") + "\")");
                            buttonIntList[k] = 0;
                        }
                    }
                    System.out.print(bindType + " Controller (port " + controllerElement.getAttribute("port") + ") button " + controllerBindElement.getTextContent());
                    if (!activeFunc.equals("")) { System.out.print(" actively bound to " + activeFunc); }
                    if (!inactiveFunc.equals("")) { System.out.print(" inactively bound to " + inactiveFunc); }
                    System.out.println("");
                    TRCInputManager.registerButtonBind(controller, buttonIntList, activeFunc, inactiveFunc);
                }
                else if (bindType.equals("axis"))
                {
                    System.out.println(controllerElement.getAttribute("type") + " controller (port " + controllerElement.getAttribute("port") + ") axis " + controllerBindElement.getTextContent() + " assigned to " + controllerBindElement.getAttribute("name"));
                    int axisNum = -1;
                    try
                    {
                        axisNum = Integer.parseInt(controllerBindElement.getTextContent());
                    }
                    catch (NumberFormatException e)
                    {
                        try
                        {
                            if (controller.getType() == ControllerType.Xbox360)
                            {
                                axisNum = XboxController.Axis.valueOf(controllerBindElement.getTextContent()).value;
                            }
                            else //(controller.getType() == ControllerType.Extreme3D)
                            {
                                axisNum = Joystick.AxisType.valueOf(controllerBindElement.getTextContent()).value;
                            }
                        } catch (Exception e2) {}
                    }
                    if (axisNum == -1)
                    {
                        System.out.println("Error interpreting axis bind \"" + controllerBindElement.getTextContent() + "\" (" + controllerElement.getAttribute("type") + " controller on port " + controllerElement.getAttribute("port") + ", name \"" + controllerBindElement.getAttribute("name") + "\")");
                        axisNum = 0;
                    }

                    TRCInputManager.registerAxisBind(controller, axisNum, controllerBindElement.getAttribute("name"));
                }
                else if (bindType.equals("dps"))
                {
                    System.out.println(controllerElement.getAttribute("type") + " controller (port " + controllerElement.getAttribute("port") + ") dps (axis " + controllerBindElement.getTextContent() + ") assigned to " + controllerBindElement.getAttribute("name"));
                    
                    Object[][] axes = new Object[3][];
                    String[] axesText = controllerBindElement.getTextContent().split(",", 0);
                    for (int a = 0; a < 3; a++)
                    {
                        axes[a] = new Object[2];
                        axes[a][0] = controller;
                        axes[a][1] = -1;
                        String axisText = axesText[a];
                        try
                        {
                            axes[a][1] = Integer.parseInt(axisText);
                        }
                        catch (NumberFormatException e)
                        {
                            try
                            {
                                if (controller.getType() == ControllerType.Xbox360)
                                {
                                    axes[a][1] = XboxController.Axis.valueOf(axisText).value;
                                }
                                else
                                {
                                    axes[a][1] = Joystick.AxisType.valueOf(axisText).value;
                                }
                            } catch (Exception e2) {}
                        }
                        if ((int) axes[a][1] == -1)
                        {
                            if ((int) TRCInputManager.getAxisID(axisText)[1] != -1)
                            {
                                axes[a] = TRCInputManager.getAxisID(axisText);
                            }
                            else
                            {
                                System.out.println("Error interpreting axis bind \"" + axisText + "\" (" + controllerElement.getAttribute("type") + " controller on port " + controllerElement.getAttribute("port") + ", name \"" + controllerBindElement.getAttribute("name") + "\")");
                                axes[a][1] = 0;
                            }
                        }
                    }

                    TRCInputManager.registerDPSBind(axes, controllerBindElement.getAttribute("name"));
                }
            }
        }
    }

    public static void parseAutoConfig(String configString)
    {
        try
        {
            doc = builder.parse(new InputSource(new StringReader(configString)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Element root = doc.getDocumentElement();
        NodeList autoPathNodeList = root.getElementsByTagName("auto-path");
        for (int i = 0; i < autoPathNodeList.getLength(); i++)
        {
            Node autoPathNode = autoPathNodeList.item(i);
            Element autoPathElement = (Element) autoPathNode;
            String name = autoPathElement.getAttribute("name");
            NodeList translationNodeList = autoPathElement.getElementsByTagName("translation");

            Node startNode = autoPathElement.getElementsByTagName("start").item(0);
            Element startElement = (Element) startNode;
            double x = safeParseDouble(startElement.getAttribute("x"));
            double y = safeParseDouble(startElement.getAttribute("y"));
            double angle = safeParseDouble(startElement.getAttribute("angle"));
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(angle))
            {
                System.out.println("Invalid start within auto-path \"" + name + "\", one of x: " + startElement.getAttribute("x") + ", y: " + startElement.getAttribute("y") + ", or " + startElement.getAttribute("angle") + " is not a good double.");
                continue;
            }
            Pose2d startPose = new Pose2d(x, y, Rotation2d.fromDegrees(angle));

            Node endNode = autoPathElement.getElementsByTagName("end").item(0);
            Element endElement = (Element) endNode;
            x = safeParseDouble(endElement.getAttribute("x"));
            y = safeParseDouble(endElement.getAttribute("y"));
            angle = safeParseDouble(endElement.getAttribute("angle"));
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(angle))
            {
                System.out.println("Invalid end within auto-path \"" + name + "\", one of x: " + endElement.getAttribute("x") + ", y: " + endElement.getAttribute("y") + ", or " + endElement.getAttribute("angle") + " is not a good double.");
                continue;
            }
            Pose2d endPose = new Pose2d(x, y, Rotation2d.fromDegrees(angle));

            ArrayList<Translation2d> translationArray = new ArrayList<Translation2d>();
            for (int j = 0; j < translationNodeList.getLength(); j++)
            {
                if (translationNodeList.item(j).getNodeType() != Node.ELEMENT_NODE) { continue; }
                Element translationElement = (Element) translationNodeList.item(j);

                int id = -1;
                try
                {
                    id = Integer.parseInt(translationElement.getAttribute("id"));
                }
                catch (Exception e)
                {
                    System.out.println("Invalid translation within auto-path \"" + name + "\", does not have an id.");
                    continue;
                }

                x = safeParseDouble(translationElement.getAttribute("x"));
                y = safeParseDouble(translationElement.getAttribute("y"));
                if (Double.isNaN(x) || Double.isNaN(y))
                {
                    System.out.println("Invalid translation within auto-path \"" + name + "\", one of x: " + translationElement.getAttribute("x") + " or y: " + translationElement.getAttribute("y") + " is not a good double.");
                    continue;
                }

                translationArray.set(id, new Translation2d(x, y));
            }

            TRCAutoPath path = new TRCAutoPath(startPose, translationArray.subList(0, translationArray.size()), endPose);
            TRCAutoManager.registerPath(name, path);
        }

        NodeList autoRoutineNodeList = root.getElementsByTagName("auto-routine");
        for (int i = 0; i < autoRoutineNodeList.getLength(); i++)
        {
            TRCAutoRoutine routine = new TRCAutoRoutine();
            Node autoRoutineNode = autoRoutineNodeList.item(i);
            Element autoRoutineElement = (Element) autoRoutineNode;
            String name = autoRoutineElement.getAttribute("name");
            NodeList eventNodeList = autoRoutineElement.getElementsByTagName("event");
            for (int j = 0; j < eventNodeList.getLength(); j++)
            {
                if (eventNodeList.item(j).getNodeType() != Node.ELEMENT_NODE) { continue; }
                Element eventBindElement = (Element) eventNodeList.item(j);
                double startTime = safeParseDouble(eventBindElement.getAttribute("start"));
                if (Double.isNaN(startTime))
                {
                    System.out.println("Invalid event within auto-routine \"" + name + "\", start time \"" + eventBindElement.getAttribute("start") + "\" is not a good double.");
                    continue;
                }
                double length = safeParseDouble(eventBindElement.getAttribute("length"));
                Object[] params = parseParamList(eventBindElement.getAttribute("parameters"));
                boolean nonblocking = Boolean.parseBoolean(eventBindElement.getAttribute("nonblocking"));
                String func = eventBindElement.getTextContent();
                TRCAutoEvent event = new TRCAutoEvent(startTime, length, params, nonblocking, func);
                routine.addEvent(event);
            }
            TRCAutoManager.registerRoutine(name, routine);
        }
    }

    private static Object[] parseParamList(String stringParams)
    {
        String[] splitStringParams = stringParams.split(",");
        Object[] convertedParams = new Object[splitStringParams.length];

        for (int i = 0; i < splitStringParams.length; i++)
        {
            String s = splitStringParams[i];
            try
            {
                convertedParams[i] = Integer.parseInt(s);
            }
            catch (Exception e)
            {
                try
                {
                    convertedParams[i] = Double.parseDouble(s);
                }
                catch (Exception e2)
                {
                    convertedParams[i] = s;
                }
            }
        }

        return convertedParams;
    }

    private static double safeParseDouble(String s)
    {
        try
        {
            return Double.parseDouble(s);
        }
        catch (Exception e)
        {
            return Double.NaN;
        }
    }
}