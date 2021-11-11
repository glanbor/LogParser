package task3913;



import task3913.query.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogParser implements IPQuery, UserQuery, DateQuery, EventQuery, QLQuery {
    private Path logDir;
    private List<Log> logs;

    public LogParser(Path logDir) {
        this.logDir = logDir;
        logs = getLogs();
    }

    public List<Log> getLogs() {
        logs = new ArrayList<>();
        for (String string : getStringLogs()) {
            String[] strings = string.split("\t");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            try {
                Date date = simpleDateFormat.parse(strings[2]);
                String[] s = strings[3].split(" ");
                int taskNumber = 0;
                if (s.length > 1) taskNumber = Integer.parseInt(s[1]);
                logs.add(new Log(strings[0], strings[1], date, Event.valueOf(s[0]), taskNumber, Status.valueOf(strings[4])));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return logs;
    }

    public List<String> getStringLogs() {
        List<String> stringLogs = new ArrayList<>();
        try {
            Stream<Path> stream = Files.list(logDir);
            List<Path> paths = new ArrayList<>();
            paths = stream.filter(x -> x.getFileName().toString().endsWith(".log")).collect(Collectors.toList());
            for (Path path : paths) {
                Files.lines(path).forEach(stringLogs::add);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringLogs;
    }

    public Predicate<Log> getDatesInterval(Date after, Date before) {
        Predicate<Log> predicate = null;
        if (before == null && after != null)
            predicate = x -> x.getDate().getTime() >= after.getTime();
        else if (before != null && after == null)
            predicate = x -> x.getDate().getTime() <= before.getTime();
        else if (before == null && after == null)
            predicate = x -> true;
        else predicate = x -> x.getDate().getTime() >= after.getTime() &&
                    x.getDate().getTime() <= before.getTime();
        return predicate;
    }

    @Override
    public int getNumberOfUniqueIPs(Date after, Date before) {
        return getUniqueIPs(after, before).size();
    }

    @Override
    public Set<String> getUniqueIPs(Date after, Date before) {
        Set<String> ips = logs.stream().filter(getDatesInterval(after, before))
                .distinct()
                .map(Log::getIp)
                .collect(Collectors.toSet());
        return ips;
    }

    @Override
    public Set<String> getIPsForUser(String user, Date after, Date before) {
        Set<String> ips = logs.stream().filter(x -> x.getUser().equals(user))
                .filter(getDatesInterval(after, before))
                .distinct()
                .map(Log::getIp)
                .collect(Collectors.toSet());
        return ips;
    }

    @Override
    public Set<String> getIPsForEvent(Event event, Date after, Date before) {
        Set<String> ips = logs.stream().filter(x -> x.getEvent() == event)
                .filter(getDatesInterval(after, before))
                .distinct()
                .map(Log::getIp)
                .collect(Collectors.toSet());
        return ips;
    }

    @Override
    public Set<String> getIPsForStatus(Status status, Date after, Date before) {
        Set<String> ips = logs.stream().filter(x -> x.getStatus() == status)
                .filter(getDatesInterval(after, before))
                .distinct()
                .map(Log::getIp)
                .collect(Collectors.toSet());
        return ips;
    }

    @Override
    public Set<String> getAllUsers() {
        Set<String> users = logs.stream().distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return users;
    }

    @Override
    public int getNumberOfUsers(Date after, Date before) {
        Set<String> users = logs.stream().filter(getDatesInterval(after, before))
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return users.size();
    }

    @Override
    public int getNumberOfUserEvents(String user, Date after, Date before) {
        Set<Event> events = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getUser().equals(user))
                .distinct()
                .map(Log::getEvent)
                .collect(Collectors.toSet());
        return events.size();
    }

    @Override
    public Set<String> getUsersForIP(String ip, Date after, Date before) {
        Set<String> users = logs.stream().filter(x -> x.getIp().equals(ip))
                .filter(getDatesInterval(after, before))
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return users;
    }

    @Override
    public Set<String> getLoggedUsers(Date after, Date before) {
        Set<String> loggedUsers = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.LOGIN)
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return loggedUsers;
    }

    @Override
    public Set<String> getDownloadedPluginUsers(Date after, Date before) {
        Set<String> downloadedUsers = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.DOWNLOAD_PLUGIN)
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return downloadedUsers;
    }

    @Override
    public Set<String> getWroteMessageUsers(Date after, Date before) {
        Set<String> users = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.WRITE_MESSAGE)
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return users;
    }

    @Override
    public Set<String> getSolvedTaskUsers(Date after, Date before) {
        Set<String> users = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.SOLVE_TASK)
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return users;
    }

    @Override
    public Set<String> getSolvedTaskUsers(Date after, Date before, int task) {
        Set<String> users = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.SOLVE_TASK)
                .filter(x -> x.getTaskNumber() == task)
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return users;
    }

    @Override
    public Set<String> getDoneTaskUsers(Date after, Date before) {
        Set<String> users = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.DONE_TASK)
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return users;
    }

    @Override
    public Set<String> getDoneTaskUsers(Date after, Date before, int task) {
        Set<String> users = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.DONE_TASK)
                .filter(x -> x.getTaskNumber() == task)
                .distinct()
                .map(Log::getUser)
                .collect(Collectors.toSet());
        return users;
    }

    @Override
    public Set<Date> getDatesForUserAndEvent(String user, Event event, Date after, Date before) {
        Set<Date> dates = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getUser().equals(user))
                .filter(x -> x.getEvent() == event)
                .map(Log::getDate)
                .collect(Collectors.toSet());
        return dates;
    }

    @Override
    public Set<Date> getDatesWhenSomethingFailed(Date after, Date before) {
        Set<Date> dates = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getStatus() == Status.FAILED)
                .map(Log::getDate)
                .collect(Collectors.toSet());
        return dates;
    }

    @Override
    public Set<Date> getDatesWhenErrorHappened(Date after, Date before) {
        Set<Date> dates = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getStatus() == Status.ERROR)
                .map(Log::getDate)
                .collect(Collectors.toSet());
        return dates;
    }

    @Override
    public Date getDateWhenUserLoggedFirstTime(String user, Date after, Date before) {
        Date date = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getUser().equals(user))
                .filter(x -> x.getEvent() == Event.LOGIN)
                .map(Log::getDate)
                .sorted()
                .collect(Collectors.toCollection(TreeSet::new))
                .pollFirst();
        return date == null ? null : date;
    }

    @Override
    public Date getDateWhenUserSolvedTask(String user, int task, Date after, Date before) {
        TreeSet<Date> set = new TreeSet<>(logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getUser().equals(user))
                .filter(x -> x.getEvent() == Event.SOLVE_TASK)
                .filter(x -> x.getTaskNumber() == task)
                .map(Log::getDate)
                .collect(Collectors.toSet()));
        return set.size() == 0 ? null : set.iterator().next();
    }

    @Override
    public Date getDateWhenUserDoneTask(String user, int task, Date after, Date before) {
        Date date = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getUser().equals(user))
                .filter(x -> x.getEvent() == Event.DONE_TASK)
                .filter(x -> x.getTaskNumber() == task)
                .map(Log::getDate)
                .collect(Collectors.toCollection(TreeSet::new))
                .pollFirst();
        return date == null ? null : date;
    }

    @Override
    public Set<Date> getDatesWhenUserWroteMessage(String user, Date after, Date before) {
        Set<Date> dates = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getUser().equals(user))
                .filter(x -> x.getEvent() == Event.WRITE_MESSAGE)
                .map(Log::getDate)
                .collect(Collectors.toSet());
        return dates;
    }

    @Override
    public Set<Date> getDatesWhenUserDownloadedPlugin(String user, Date after, Date before) {
        Set<Date> dates = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getUser().equals(user))
                .filter(x -> x.getEvent() == Event.DOWNLOAD_PLUGIN)
                .map(Log::getDate)
                .collect(Collectors.toSet());
        return dates;
    }

    @Override
    public int getNumberOfAllEvents(Date after, Date before) {
        return getAllEvents(after, before).size();
    }

    @Override
    public Set<Event> getAllEvents(Date after, Date before) {
        Set<Event> events = logs.stream().filter(getDatesInterval(after, before))
                .distinct()
                .map(Log::getEvent)
                .collect(Collectors.toSet());
        return events;
    }

    @Override
    public Set<Event> getEventsForIP(String ip, Date after, Date before) {
        Set<Event> events = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getIp().equals(ip))
                .distinct()
                .map(Log::getEvent)
                .collect(Collectors.toSet());
        return events;
    }

    @Override
    public Set<Event> getEventsForUser(String user, Date after, Date before) {
        Set<Event> events = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getUser().equals(user))
                .distinct()
                .map(Log::getEvent)
                .collect(Collectors.toSet());
        return events;
    }

    @Override
    public Set<Event> getFailedEvents(Date after, Date before) {
        Set<Event> events = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getStatus() == Status.FAILED)
                .distinct()
                .map(Log::getEvent)
                .collect(Collectors.toSet());
        return events;
    }

    @Override
    public Set<Event> getErrorEvents(Date after, Date before) {
        Set<Event> events = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getStatus() == Status.ERROR)
                .distinct()
                .map(Log::getEvent)
                .collect(Collectors.toSet());
        return events;
    }

    @Override
    public int getNumberOfAttemptToSolveTask(int task, Date after, Date before) {
        Long number = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.SOLVE_TASK)
                .filter(x -> x.getTaskNumber() == task)
                .collect(Collectors.counting());
        return Math.toIntExact(number);
    }

    @Override
    public int getNumberOfSuccessfulAttemptToSolveTask(int task, Date after, Date before) {
        Long number = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.DONE_TASK)
                .filter(x -> x.getTaskNumber() == task)
                .collect(Collectors.counting());
        return Math.toIntExact(number);
    }

    @Override
    public Map<Integer, Integer> getAllSolvedTasksAndTheirNumber(Date after, Date before) {
        Map<Integer, Integer> map = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.SOLVE_TASK)
                .collect(Collectors.toMap(x -> x.getTaskNumber(), x -> 1, Integer::sum));
        return map;
    }

    @Override
    public Map<Integer, Integer> getAllDoneTasksAndTheirNumber(Date after, Date before) {
        Map<Integer, Integer> map = logs.stream().filter(getDatesInterval(after, before))
                .filter(x -> x.getEvent() == Event.DONE_TASK)
                .collect(Collectors.toMap(x -> x.getTaskNumber(), x -> 1, Integer::sum));
        return map;
    }
    public Set<Date> getUniqDates() {
        return logs.stream().distinct()
                .map(Log::getDate).collect(Collectors.toSet());
    }
    public Set<Status> getUniqStatuses() {
        return logs.stream().distinct()
                .map(Log::getStatus).collect(Collectors.toSet());
    }
    public Function<? super Log, ? extends Object> getFunction(String field) {
            switch (field) {
                case "ip":
                    return Log::getIp;
                case "user":
                    return Log::getUser;
                case "date":
                    return Log::getDate;
                case "event":
                    return Log::getEvent;
                case "status":
                    return Log::getStatus;
                default:
                    return null;
        }
    }
   public Predicate<Log> getPredicate(String field, Object valueObject) {
        switch (field) {
            case "ip":
                return x -> x.getIp().equals(valueObject);
            case "user":
                return x -> x.getUser().equals(valueObject);
            case "date":
                return x -> x.getDate().equals(valueObject);
            case "event":
                return x -> x.getEvent().equals(valueObject);
            case "status":
                return x -> x.getStatus().equals(valueObject);
            default:
                return null;
        }
    }

    @Override
    public Set<Object> execute(String query) {
        String[] strings = query.split(" ");
        if (query.length() < 11) {
            Set<Object> set = new HashSet<>(logs.stream().map(getFunction(strings[1]))
                    .collect(Collectors.toSet()));
            return set;
        }
        else  {
            String[] strings2 = query.split("\"");
            String valueString = strings2[1].trim();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            Date after = null;
            Date before = null;
            try {
                if (strings2.length > 2) {
                    after = simpleDateFormat.parse(strings2[3]);
                    before = simpleDateFormat.parse(strings2[5]);
                    if (after != null) after = new Date(after.getTime() + 1L);
                    if (before != null) before = new Date(before.getTime() - 1L);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Object valueObject;
            if (strings[3].equals("event"))
                valueObject = Event.valueOf(valueString);
            else if (strings[3].equals("status"))
                valueObject = Status.valueOf(valueString);
            else if (strings[3].equals("date")) {
                try {
                    Date date = simpleDateFormat.parse(valueString);
                    valueObject = date;
                } catch (ParseException e) {
                    e.printStackTrace();
                    valueObject = null;
                }
            }
            else valueObject = valueString;
            Object finalValueObject = valueObject;
            return logs.stream()
                    .filter(getDatesInterval(after, before))
                    .filter(getPredicate(strings[3],valueObject))
                    .map(getFunction(strings[1]))
                    .collect(Collectors.toSet());
        }
    }
}