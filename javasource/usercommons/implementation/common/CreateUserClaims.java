package usercommons.implementation.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IMendixObjectMember;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;

import usercommons.proxies.UserClaim;

public class CreateUserClaims {
	public static java.util.List<IMendixObject> createUserClaims(IMendixObject mo, IContext context) {
		Map<String, ? extends IMendixObjectMember<?>> members = mo.getMembers(context);
		List<IMendixObject> UserClaims = new ArrayList();
		for (String key : members.keySet()) {
			IMendixObjectMember<?> m = members.get(key);
			String name = m.getName();
			Object obj = m.getValue(context);
			if (obj != null) {
				IMendixObject ins = Core.instantiate(context, UserClaim.getType());
				ins.setValue(context, UserClaim.MemberNames.Name.toString(), name);
				IMetaPrimitive metaPrimitive = Core.getMetaObject(mo.getType()).getMetaPrimitive(m.getName());
				if (metaPrimitive != null && metaPrimitive.getType() == IMetaPrimitive.PrimitiveType.DateTime) {
					ins.setValue(context, UserClaim.MemberNames.DateValue.toString(), obj);
				} else {
					ins.setValue(context, UserClaim.MemberNames.Value.toString(), String.valueOf(obj));

				}
				UserClaims.add(ins);
			}
		}
		return UserClaims;
	}
}
