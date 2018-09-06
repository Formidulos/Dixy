package com.avairebot.middleware;

import com.avairebot.AvaIre;
import com.avairebot.contracts.middleware.Middleware;
import com.avairebot.factories.MessageFactory;
import com.avairebot.middleware.permission.PermissionCheck;
import com.avairebot.middleware.permission.PermissionCommon;
import com.avairebot.permissions.Permissions;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RequirePermissionMiddleware extends Middleware {

    public RequirePermissionMiddleware(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String buildHelpDescription(@Nonnull String[] arguments) {
        arguments = Arrays.copyOfRange(arguments, 1, arguments.length);
        if (arguments.length == 1) {
            return PermissionCommon.formatWithOneArgument(arguments[0]);
        }
        return String.format("**The `%s` permissions is required to use this command!**",
            Arrays.stream(arguments)
                .map(Permissions::fromNode)
                .map(Permissions::getPermission)
                .map(Permission::getName)
                .collect(Collectors.joining("`, `"))
        );
    }

    @Override
    public boolean handle(@Nonnull Message message, @Nonnull MiddlewareStack stack, String... args) {
        if (!message.getChannelType().isGuild()) {
            return stack.next();
        }

        if (args.length < 2) {
            AvaIre.getLogger().warn(String.format(
                "\"%s\" is parsing invalid amount of arguments to the require middleware, 2 arguments are required.", stack.getCommand()
            ));
            return stack.next();
        }

        PermissionCheck permissionCheck = new PermissionCheck(message, args);
        if (!permissionCheck.check(stack)) {
            return false;
        }

        if (!permissionCheck.getMissingUserPermissions().isEmpty()) {
            MessageFactory.makeError(message, "You're missing the required permission node for this command:\n`:permission`")
                .set("permission", permissionCheck.getMissingUserPermissions().stream()
                    .map(Permissions::getPermission)
                    .map(Permission::getName)
                    .collect(Collectors.joining("`, `"))
                ).queue();
        }

        if (!permissionCheck.getMissingBotPermissions().isEmpty()) {
            MessageFactory.makeError(message, "I'm missing the following permission to run this command successfully:\n`:permission`")
                .set("permission", permissionCheck.getMissingBotPermissions().stream()
                    .map(Permissions::getPermission)
                    .map(Permission::getName)
                    .collect(Collectors.joining("`, `"))
                ).queue();
        }

        return permissionCheck.isEmpty() && stack.next();
    }
}
