package externaldatabaseconnector.mx.impl.service;

import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;

import externaldatabaseconnector.mx.impl.DatabaseConnectorException;
import externaldatabaseconnector.mx.interfaces.ObjectInstantiator;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class MendixObjectService {

    private final ILogNode logNode;
    private final ObjectInstantiator objectInstantiator;

    public MendixObjectService(ILogNode logNode, ObjectInstantiator objectInstantiator) {
        this.logNode = logNode;
        this.objectInstantiator = objectInstantiator;
    }

    //Input parameters CallableMainEntityMendixObject, CallableResultSetAssociation have been used to form association between the Stored procedure main and the result set entity
    public List<IMendixObject> createMendixObjects(final IContext context,
                                                   final IMetaObject metaObject, List<Map<String, Optional<Object>>> resultSet, IMetaAssociation callableResultSetAssociation, IMendixObject callableMainEntityMendixObject) throws
        DatabaseConnectorException {
        List<IMendixObject> convertedResult = new ArrayList<>();
        for (Map<String, Optional<Object>> result : resultSet) {
            convertedResult.add(createMendixObject(context, metaObject, result, callableResultSetAssociation, callableMainEntityMendixObject));
        }
        return convertedResult;
    }

    private IMendixObject createMendixObject(final IContext context,
                                            final IMetaObject metaObject, Map<String, Optional<Object>> columns, IMetaAssociation callableResultSetAssociation, IMendixObject callableMainEntityMendixObject) throws
        DatabaseConnectorException {
        IMendixObject obj = objectInstantiator.instantiate(context, metaObject.getName());

        for (Map.Entry<String, Optional<Object>> column : columns.entrySet()) {
            setMemberValue(context, metaObject, obj, column.getKey(), column.getValue());
        }

        if (callableResultSetAssociation != null && callableMainEntityMendixObject != null) {
            obj.setValue(context, callableResultSetAssociation.getName(), callableMainEntityMendixObject.getId());
        }

        return obj;
    }

    public IMendixObject createMendixObject(final IContext context, final IMetaObject metaObject){
        return objectInstantiator.instantiate(context, metaObject.getName());
    }

    public void setMemberValue(IContext context, IMetaObject metaObject, IMendixObject obj, String name, Optional<Object> value) throws
        DatabaseConnectorException {
        IMetaPrimitive metaPrimitive = metaObject.getMetaPrimitive(name);
        if (metaPrimitive == null) {
            String errorMessage = String.format(
                    "Database attribute '%1$s' is not in the entity '%2$s'."
                            + " Please check the entity '%2$s' attribute names with the database column names.",
                    name, metaObject.getName());
            logNode.error(errorMessage);
            throw new DatabaseConnectorException(errorMessage);
        }
        IMetaPrimitive.PrimitiveType type = metaPrimitive.getType();
        // convert to suitable value (different for Binary type)
        Function<Object, Object> toSuitableValue = toSuitableValue(type);
        // for Boolean type, convert null to false
        Supplier<Object> defaultValue = () -> type == IMetaPrimitive.PrimitiveType.Boolean ? Boolean.FALSE : null;
        // apply two functions declared above
        Object convertedValue = value.map(toSuitableValue).orElseGet(defaultValue);
        // update object with converted value
        if (type == IMetaPrimitive.PrimitiveType.HashString)
            throw new DatabaseConnectorException(String.format(
                    "Attribute type Hashed String for attribute '%1$s' on entity '%2$s' is not supported, "
                            + "please use attribute type 'String' instead",
                    name, metaObject.getName()));
        else
            obj.setValue(context, name, convertedValue);
    }

    private Function<Object, Object> toSuitableValue(final IMetaPrimitive.PrimitiveType type) {
        return v -> type == IMetaPrimitive.PrimitiveType.Binary ? new ByteArrayInputStream((byte[]) v) : v;
    }
}