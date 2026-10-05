package com.napier.sem;

import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class App
{
    public static void main(String[] args)
    {
        // Create new Application
        App a = new App();

        // Connect to database
        if (args.length < 1)
        {
            // Running locally (e.g. from IntelliJ) against the Docker database
            a.connect("localhost:33070", 0);
        }
        else
        {
            // Running in Docker: location and startup delay passed in
            a.connect(args[0], Integer.parseInt(args[1]));
        }

        // Show menu until the user exits
        a.runMenu();

        // Disconnect from database
        a.disconnect();
    }

    /**
     * Connection to MySQL database.
     */
    private Connection con = null;

    /**
     * Connect to the MySQL database.
     *
     * @param location host:port of the database server
     * @param delay    milliseconds to wait before each connection attempt
     */
    public void connect(String location, int delay)
    {
        try
        {
            // Load Database driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Could not load SQL driver");
            System.exit(-1);
        }

        int retries = 10;
        for (int i = 0; i < retries; ++i)
        {
            System.out.println("Connecting to database...");
            try
            {
                // Wait a bit for db to start
                Thread.sleep(delay);
                // Connect to database
                con = DriverManager.getConnection("jdbc:mysql://" + location + "/employees?allowPublicKeyRetrieval=true&useSSL=false", "root", "example");
                System.out.println("Successfully connected");
                break;
            }
            catch (SQLException sqle)
            {
                System.out.println("Failed to connect to database attempt " + Integer.toString(i));
                System.out.println(sqle.getMessage());
            }
            catch (InterruptedException ie)
            {
                System.out.println("Thread interrupted? Should not happen.");
            }
        }
    }

    /**
     * Disconnect from the MySQL database.
     */
    public void disconnect()
    {
        if (con != null)
        {
            try
            {
                // Close connection
                con.close();
            }
            catch (Exception e)
            {
                System.out.println("Error closing connection to database");
            }
        }
    }

    /**
     * Get an employee's details by employee number.
     *
     * @param ID employee number to look up
     * @return the matching Employee, or null if not found or on error
     */
    public Employee getEmployee(int ID)
    {
        try
        {
            // Create an SQL statement
            Statement stmt = con.createStatement();
            // Create string for SQL statement
            String strSelect =
                    "SELECT emp_no, first_name, last_name "
                    + "FROM employees "
                    + "WHERE emp_no = " + ID;
            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(strSelect);
            // Return new employee if valid.
            // Check one is returned
            if (rset.next())
            {
                Employee emp = new Employee();
                emp.emp_no = rset.getInt("emp_no");
                emp.first_name = rset.getString("first_name");
                emp.last_name = rset.getString("last_name");
                return emp;
            }
            else
                return null;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            System.out.println("Failed to get employee details");
            return null;
        }
    }

    public void displayEmployee(Employee emp)
    {
        if (emp != null)
        {
            System.out.println(
                    emp.emp_no + " "
                            + emp.first_name + " "
                            + emp.last_name + "\n"
                            + emp.title + "\n"
                            + "Salary:" + emp.salary + "\n"
                            + emp.dept_name + "\n"
                            + "Manager: " + emp.manager + "\n");
        }
    }

    /**
     * Show a menu of reports and run the one the user picks.
     * Stops when the user chooses Exit or input runs out.
     */
    public void runMenu()
    {
        Scanner in = new Scanner(System.in);
        while (true)
        {
            System.out.println();
            System.out.println("===== Employee Reports =====");
            System.out.println("1. Salary report (all current salaries)");
            System.out.println("2. View all employees");
            System.out.println("3. View employee by ID");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            // No more input (e.g. running in Docker without a terminal)
            if (!in.hasNextLine())
                return;
            String choice = in.nextLine().trim();

            switch (choice)
            {
                case "1":
                    printSalaries(getAllSalaries());
                    break;
                case "2":
                    printEmployees(getAllEmployees());
                    break;
                case "3":
                    System.out.print("Enter employee number: ");
                    if (!in.hasNextLine())
                        return;
                    try
                    {
                        int id = Integer.parseInt(in.nextLine().trim());
                        Employee emp = getEmployee(id);
                        if (emp == null)
                            System.out.println("No employee found with number " + id);
                        else
                            displayEmployee(emp);
                    }
                    catch (NumberFormatException e)
                    {
                        System.out.println("Employee number must be a whole number");
                    }
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option, please try again");
            }
        }
    }

    /**
     * Get the current salary of every employee.
     *
     * @return list of employees with salaries, or null on error
     */
    public ArrayList<Employee> getAllSalaries()
    {
        try
        {
            // Create an SQL statement
            Statement stmt = con.createStatement();
            // Create string for SQL statement
            String strSelect =
                    "SELECT employees.emp_no, employees.first_name, employees.last_name, salaries.salary "
                    + "FROM employees, salaries "
                    + "WHERE employees.emp_no = salaries.emp_no AND salaries.to_date = '9999-01-01' "
                    + "ORDER BY employees.emp_no ASC";
            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(strSelect);
            // Extract employee information
            ArrayList<Employee> employees = new ArrayList<Employee>();
            while (rset.next())
            {
                Employee emp = new Employee();
                emp.emp_no = rset.getInt("emp_no");
                emp.first_name = rset.getString("first_name");
                emp.last_name = rset.getString("last_name");
                emp.salary = rset.getInt("salary");
                employees.add(emp);
            }
            return employees;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            System.out.println("Failed to get salary details");
            return null;
        }
    }

    /**
     * Get every employee.
     *
     * @return list of all employees, or null on error
     */
    public ArrayList<Employee> getAllEmployees()
    {
        try
        {
            // Create an SQL statement
            Statement stmt = con.createStatement();
            // Create string for SQL statement
            String strSelect =
                    "SELECT emp_no, first_name, last_name "
                    + "FROM employees "
                    + "ORDER BY emp_no ASC";
            // Execute SQL statement
            ResultSet rset = stmt.executeQuery(strSelect);
            // Extract employee information
            ArrayList<Employee> employees = new ArrayList<Employee>();
            while (rset.next())
            {
                Employee emp = new Employee();
                emp.emp_no = rset.getInt("emp_no");
                emp.first_name = rset.getString("first_name");
                emp.last_name = rset.getString("last_name");
                employees.add(emp);
            }
            return employees;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            System.out.println("Failed to get employee list");
            return null;
        }
    }

    /**
     * Print a salary report.
     *
     * @param employees the employees to print
     */
    public void printSalaries(ArrayList<Employee> employees)
    {
        if (employees == null)
            return;
        // Print header
        System.out.println(String.format("%-10s %-15s %-20s %-8s", "Emp No", "First Name", "Last Name", "Salary"));
        // Loop over all employees in the list
        for (Employee emp : employees)
        {
            System.out.println(String.format("%-10s %-15s %-20s %-8s",
                    emp.emp_no, emp.first_name, emp.last_name, emp.salary));
        }
        System.out.println(employees.size() + " employees");
    }

    /**
     * Print a list of employees.
     *
     * @param employees the employees to print
     */
    public void printEmployees(ArrayList<Employee> employees)
    {
        if (employees == null)
            return;
        // Print header
        System.out.println(String.format("%-10s %-15s %-20s", "Emp No", "First Name", "Last Name"));
        // Loop over all employees in the list
        for (Employee emp : employees)
        {
            System.out.println(String.format("%-10s %-15s %-20s",
                    emp.emp_no, emp.first_name, emp.last_name));
        }
        System.out.println(employees.size() + " employees");
    }
}
